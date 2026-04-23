package com.marrylink;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.marrylink.common.Result;
import com.marrylink.controller.CommissionController;
import com.marrylink.controller.HostWalletController;
import com.marrylink.controller.PaymentController;
import com.marrylink.controller.SettlementController;
import com.marrylink.entity.*;
import com.marrylink.service.*;
import com.marrylink.utils.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the payment/commission/settlement flow.
 * Uses plain JUnit 5 + Mockito (no Spring context needed).
 */
@ExtendWith(MockitoExtension.class)
class PaymentCommissionFlowTest {

    // ---- PaymentController dependencies ----
    @Mock private IPlatformEscrowService platformEscrowService;
    @Mock private IOrderService orderService;
    @Mock private IOrderLogService orderLogService;
    @InjectMocks private PaymentController paymentController;

    // ---- SettlementController dependencies ----
    @Mock private ISettlementService settlementService;
    @Mock private IPlatformSettingsService platformSettingsService;
    @Mock private IHostWalletService hostWalletService;
    @Mock private ICommissionOrderService commissionOrderService;
    @Mock private IMessageService messageService;
    // orderService and orderLogService are shared
    // We need a second set for SettlementController
    private SettlementController settlementController;

    // ---- CommissionController dependencies ----
    @Mock private IHostService hostService;
    private CommissionController commissionController;

    // ---- HostWalletController ----
    @Mock private IWithdrawalRequestService withdrawalRequestService;
    private HostWalletController hostWalletController;

    @Mock private HttpServletRequest httpServletRequest;

    // SecurityUtils mock
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        // Manually inject mocks into controllers that share dependencies
        settlementController = new SettlementController();
        injectField(settlementController, "settlementService", settlementService);
        injectField(settlementController, "orderService", orderService);
        injectField(settlementController, "platformEscrowService", platformEscrowService);
        injectField(settlementController, "platformSettingsService", platformSettingsService);
        injectField(settlementController, "hostWalletService", hostWalletService);
        injectField(settlementController, "commissionOrderService", commissionOrderService);
        injectField(settlementController, "orderLogService", orderLogService);
        injectField(settlementController, "messageService", messageService);

        commissionController = new CommissionController();
        injectField(commissionController, "commissionOrderService", commissionOrderService);
        injectField(commissionController, "hostWalletService", hostWalletService);
        injectField(commissionController, "hostService", hostService);

        hostWalletController = new HostWalletController();
        injectField(hostWalletController, "hostWalletService", hostWalletService);
        injectField(hostWalletController, "withdrawalRequestService", withdrawalRequestService);

        securityUtilsMock = mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    /** Reflective field injection helper */
    private void injectField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject " + fieldName, e);
        }
    }

    // ======================== Helpers ========================

    private Order buildOrder(Long id, String orderNo, Long hostId, BigDecimal amount, int status) {
        Order o = new Order();
        o.setId(id);
        o.setOrderNo(orderNo);
        o.setHostId(hostId);
        o.setHostName("TestHost");
        o.setAmount(amount);
        o.setStatus(status);
        o.setPaymentStatus(0);
        return o;
    }

    private PlatformEscrow buildEscrow(Long id, Long orderId, BigDecimal depositAmount, BigDecimal totalOrderAmount, int status) {
        PlatformEscrow e = new PlatformEscrow();
        e.setId(id);
        e.setOrderId(orderId);
        e.setAmount(depositAmount);
        e.setTotalOrderAmount(totalOrderAmount);
        e.setStatus(status);
        return e;
    }

    private HostWallet buildWallet(Long hostId, BigDecimal balance, BigDecimal frozen) {
        HostWallet w = new HostWallet();
        w.setId(1L);
        w.setHostId(hostId);
        w.setBalance(balance);
        w.setFrozenAmount(frozen);
        w.setTotalIncome(BigDecimal.ZERO);
        w.setTotalCommission(BigDecimal.ZERO);
        w.setTotalWithdrawn(BigDecimal.ZERO);
        return w;
    }

    private CommissionOrder buildCommission(Long id, Long hostId, BigDecimal amount, int status) {
        CommissionOrder c = new CommissionOrder();
        c.setId(id);
        c.setHostId(hostId);
        c.setCommissionAmount(amount);
        c.setStatus(status);
        return c;
    }

    // ======================== 1. Payment Flow Tests ========================

    @Test
    @DisplayName("Pay order: creates escrow with 30% deposit and updates order status to 3")
    void testPayOrder_success() {
        Long orderId = 100L;
        Order order = buildOrder(orderId, "ORD001", 10L, new BigDecimal("5000.00"), 1);

        when(orderService.getById(orderId)).thenReturn(order);
        when(platformEscrowService.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(platformEscrowService.save(any(PlatformEscrow.class))).thenReturn(true);
        when(orderService.updateById(any(Order.class))).thenReturn(true);
        securityUtilsMock.when(SecurityUtils::getCurrentUsername).thenReturn("admin");
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");

        Result<Void> result = paymentController.pay(orderId, httpServletRequest);

        assertEquals("00000", result.getCode());

        // Verify escrow saved with 30% deposit amount
        ArgumentCaptor<PlatformEscrow> escrowCaptor = ArgumentCaptor.forClass(PlatformEscrow.class);
        verify(platformEscrowService).save(escrowCaptor.capture());
        PlatformEscrow savedEscrow = escrowCaptor.getValue();
        BigDecimal expectedDeposit = new BigDecimal("5000.00").multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);
        assertEquals(0, expectedDeposit.compareTo(savedEscrow.getAmount())); // 30% = 1500.00
        assertEquals(0, new BigDecimal("5000.00").compareTo(savedEscrow.getTotalOrderAmount())); // full amount stored
        assertEquals(orderId, savedEscrow.getOrderId());
        assertEquals(1, savedEscrow.getStatus());

        // Verify order status updated to 3 and paymentStatus to 1
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).updateById(orderCaptor.capture());
        Order updatedOrder = orderCaptor.getValue();
        assertEquals(3, updatedOrder.getStatus());
        assertEquals(1, updatedOrder.getPaymentStatus());
    }

    @Test
    @DisplayName("Pay order: fails when order not found")
    void testPayOrder_orderNotFound() {
        when(orderService.getById(999L)).thenReturn(null);

        Result<Void> result = paymentController.pay(999L, httpServletRequest);

        assertNotEquals("00000", result.getCode());
        assertTrue(result.getMessage().contains("不存在"));
    }

    @Test
    @DisplayName("Pay order: fails when already paid (escrow exists)")
    void testPayOrder_alreadyPaid() {
        Long orderId = 100L;
        Order order = buildOrder(orderId, "ORD001", 10L, new BigDecimal("5000.00"), 1);
        PlatformEscrow existing = buildEscrow(1L, orderId, new BigDecimal("1500.00"), new BigDecimal("5000.00"), 1);

        when(orderService.getById(orderId)).thenReturn(order);
        when(platformEscrowService.getOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        Result<Void> result = paymentController.pay(orderId, httpServletRequest);

        assertNotEquals("00000", result.getCode());
        assertTrue(result.getMessage().contains("已支付"));
        verify(platformEscrowService, never()).save(any());
    }

    // ======================== 2. Settlement Flow Tests ========================

    @Test
    @DisplayName("Settle order: full escrow to host, commission billed separately, message sent")
    void testSettleOrder_success() {
        Long orderId = 100L;
        Long hostId = 10L;
        BigDecimal orderAmount = new BigDecimal("10000.00");
        BigDecimal depositAmount = orderAmount.multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP); // 3000

        Order order = buildOrder(orderId, "ORD001", hostId, orderAmount, 4);
        PlatformEscrow escrow = buildEscrow(1L, orderId, depositAmount, orderAmount, 1);

        // Commission rate setting = 10%
        PlatformSettings rateSetting = new PlatformSettings();
        rateSetting.setSettingKey("commission_rate");
        rateSetting.setSettingValue("10.00");

        // Deadline days setting = 7
        PlatformSettings deadlineSetting = new PlatformSettings();
        deadlineSetting.setSettingKey("commission_deadline_days");
        deadlineSetting.setSettingValue("7");

        when(orderService.getById(orderId)).thenReturn(order);
        when(platformEscrowService.getOne(any(LambdaQueryWrapper.class))).thenReturn(escrow);
        when(settlementService.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(platformSettingsService.getOne(any(LambdaQueryWrapper.class)))
                .thenReturn(rateSetting)
                .thenReturn(deadlineSetting);
        when(hostWalletService.getOne(any(LambdaQueryWrapper.class))).thenReturn(null); // new wallet
        when(hostWalletService.save(any(HostWallet.class))).thenReturn(true);
        when(settlementService.save(any(Settlement.class))).thenReturn(true);
        when(platformEscrowService.updateById(any(PlatformEscrow.class))).thenReturn(true);
        when(commissionOrderService.save(any(CommissionOrder.class))).thenReturn(true);
        securityUtilsMock.when(SecurityUtils::getCurrentUsername).thenReturn("admin");

        Result<Void> result = settlementController.settle(orderId);

        assertEquals("00000", result.getCode());

        // Commission based on full order amount
        BigDecimal expectedCommission = orderAmount.multiply(new BigDecimal("10.00"))
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP); // 1000

        // Verify settlement record - netAmount = full escrow (deposit) released to host
        ArgumentCaptor<Settlement> settlementCaptor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementService).save(settlementCaptor.capture());
        Settlement savedSettlement = settlementCaptor.getValue();
        assertEquals(0, orderAmount.compareTo(savedSettlement.getAmount())); // full order amount
        assertEquals(0, expectedCommission.compareTo(savedSettlement.getCommissionAmount()));
        assertEquals(0, depositAmount.compareTo(savedSettlement.getNetAmount())); // full deposit to host
        assertEquals(2, savedSettlement.getStatus());

        // Verify wallet: balance = deposit (full escrow released), frozen = commission
        ArgumentCaptor<HostWallet> walletCaptor = ArgumentCaptor.forClass(HostWallet.class);
        verify(hostWalletService).save(walletCaptor.capture());
        HostWallet savedWallet = walletCaptor.getValue();
        assertEquals(0, depositAmount.compareTo(savedWallet.getBalance())); // full deposit
        assertEquals(0, expectedCommission.compareTo(savedWallet.getFrozenAmount()));

        // Verify commission order
        ArgumentCaptor<CommissionOrder> commissionCaptor = ArgumentCaptor.forClass(CommissionOrder.class);
        verify(commissionOrderService).save(commissionCaptor.capture());
        CommissionOrder savedCommission = commissionCaptor.getValue();
        assertEquals(0, expectedCommission.compareTo(savedCommission.getCommissionAmount()));
        assertEquals(1, savedCommission.getStatus());
        assertTrue(savedCommission.getDeadline().isAfter(LocalDateTime.now().plusDays(6)));

        // Verify commission bill message sent to host
        verify(messageService).sendCommissionBillMessage(eq(hostId), eq("ORD001"), anyString(), anyString());
    }

    @Test
    @DisplayName("Settle order: updates existing wallet with deposit amount")
    void testSettleOrder_existingWallet() {
        Long orderId = 100L;
        Long hostId = 10L;
        BigDecimal orderAmount = new BigDecimal("8000.00");
        BigDecimal depositAmount = orderAmount.multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP); // 2400

        Order order = buildOrder(orderId, "ORD001", hostId, orderAmount, 4);
        PlatformEscrow escrow = buildEscrow(1L, orderId, depositAmount, orderAmount, 1);
        HostWallet existingWallet = buildWallet(hostId, new BigDecimal("2000.00"), new BigDecimal("500.00"));
        existingWallet.setTotalIncome(new BigDecimal("5000.00"));

        PlatformSettings rateSetting = new PlatformSettings();
        rateSetting.setSettingKey("commission_rate");
        rateSetting.setSettingValue("10.00");
        PlatformSettings deadlineSetting = new PlatformSettings();
        deadlineSetting.setSettingKey("commission_deadline_days");
        deadlineSetting.setSettingValue("7");

        when(orderService.getById(orderId)).thenReturn(order);
        when(platformEscrowService.getOne(any(LambdaQueryWrapper.class))).thenReturn(escrow);
        when(settlementService.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(platformSettingsService.getOne(any(LambdaQueryWrapper.class)))
                .thenReturn(rateSetting).thenReturn(deadlineSetting);
        when(hostWalletService.getOne(any(LambdaQueryWrapper.class))).thenReturn(existingWallet);
        when(hostWalletService.updateById(any(HostWallet.class))).thenReturn(true);
        when(settlementService.save(any(Settlement.class))).thenReturn(true);
        when(platformEscrowService.updateById(any(PlatformEscrow.class))).thenReturn(true);
        when(commissionOrderService.save(any(CommissionOrder.class))).thenReturn(true);
        securityUtilsMock.when(SecurityUtils::getCurrentUsername).thenReturn("admin");

        Result<Void> result = settlementController.settle(orderId);

        assertEquals("00000", result.getCode());

        BigDecimal commissionAmt = orderAmount.multiply(new BigDecimal("10.00"))
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP); // 800

        // Wallet should have been updated (not saved as new)
        verify(hostWalletService, never()).save(any(HostWallet.class));
        verify(hostWalletService).updateById(any(HostWallet.class));

        // Balance += depositAmount (full escrow released)
        assertEquals(0, new BigDecimal("2000.00").add(depositAmount).compareTo(existingWallet.getBalance()));
        // FrozenAmount += commission
        assertEquals(0, new BigDecimal("500.00").add(commissionAmt).compareTo(existingWallet.getFrozenAmount()));
        // TotalIncome += full order amount
        assertEquals(0, new BigDecimal("5000.00").add(orderAmount).compareTo(existingWallet.getTotalIncome()));
    }

    @Test
    @DisplayName("Settle order: fails when order status is not 4")
    void testSettleOrder_orderNotCompleted() {
        Order order = buildOrder(100L, "ORD001", 10L, new BigDecimal("5000.00"), 3);
        when(orderService.getById(100L)).thenReturn(order);

        Result<Void> result = settlementController.settle(100L);

        assertNotEquals("00000", result.getCode());
        assertTrue(result.getMessage().contains("未完成"));
    }

    @Test
    @DisplayName("Settle order: fails when already settled")
    void testSettleOrder_alreadySettled() {
        Order order = buildOrder(100L, "ORD001", 10L, new BigDecimal("5000.00"), 4);
        PlatformEscrow escrow = buildEscrow(1L, 100L, new BigDecimal("1500.00"), new BigDecimal("5000.00"), 1);
        Settlement existing = new Settlement();
        existing.setId(1L);

        when(orderService.getById(100L)).thenReturn(order);
        when(platformEscrowService.getOne(any(LambdaQueryWrapper.class))).thenReturn(escrow);
        when(settlementService.getOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        Result<Void> result = settlementController.settle(100L);

        assertNotEquals("00000", result.getCode());
        assertTrue(result.getMessage().contains("已结算"));
    }

    // ======================== 3. Commission Payment Tests ========================

    @Test
    @DisplayName("Pay commission: status becomes 2, frozenAmount decreased")
    void testPayCommission_success() {
        Long commissionId = 50L;
        Long hostId = 10L;
        BigDecimal commissionAmt = new BigDecimal("1000.00");

        CommissionOrder commission = buildCommission(commissionId, hostId, commissionAmt, 1);
        HostWallet wallet = buildWallet(hostId, new BigDecimal("9000.00"), new BigDecimal("1000.00"));

        when(commissionOrderService.getById(commissionId)).thenReturn(commission);
        securityUtilsMock.when(SecurityUtils::getCurrentRefId).thenReturn(hostId);
        when(hostWalletService.getOne(any(LambdaQueryWrapper.class))).thenReturn(wallet);
        when(hostWalletService.updateById(any(HostWallet.class))).thenReturn(true);
        when(commissionOrderService.updateById(any(CommissionOrder.class))).thenReturn(true);
        Host host = new Host();
        host.setId(hostId);
        host.setCanAcceptOrder(1);
        when(hostService.getById(hostId)).thenReturn(host);

        Result<Void> result = commissionController.payCommission(commissionId);

        assertEquals("00000", result.getCode());

        // Verify frozenAmount decreased
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getFrozenAmount()));
        // Verify totalCommission increased
        assertEquals(0, commissionAmt.compareTo(wallet.getTotalCommission()));

        // Verify commission order status updated to 2
        ArgumentCaptor<CommissionOrder> captor = ArgumentCaptor.forClass(CommissionOrder.class);
        verify(commissionOrderService).updateById(captor.capture());
        assertEquals(2, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("Pay commission: fails when commission already paid (status=2)")
    void testPayCommission_alreadyPaid() {
        Long commissionId = 50L;
        Long hostId = 10L;

        CommissionOrder commission = buildCommission(commissionId, hostId, new BigDecimal("1000.00"), 2);

        when(commissionOrderService.getById(commissionId)).thenReturn(commission);
        securityUtilsMock.when(SecurityUtils::getCurrentRefId).thenReturn(hostId);

        Result<Void> result = commissionController.payCommission(commissionId);

        assertNotEquals("00000", result.getCode());
        assertTrue(result.getMessage().contains("状态异常"));
        verify(hostWalletService, never()).updateById(any());
    }

    // ======================== 4. Overdue & Ban Tests ========================

    @Test
    @DisplayName("Mark commission as overdue: status changes to 3")
    void testMarkOverdue_success() {
        Long commissionId = 50L;
        CommissionOrder commission = buildCommission(commissionId, 10L, new BigDecimal("1000.00"), 1);

        when(commissionOrderService.getById(commissionId)).thenReturn(commission);
        when(commissionOrderService.updateById(any(CommissionOrder.class))).thenReturn(true);

        Result<Void> result = commissionController.markOverdue(commissionId);

        assertEquals("00000", result.getCode());

        ArgumentCaptor<CommissionOrder> captor = ArgumentCaptor.forClass(CommissionOrder.class);
        verify(commissionOrderService).updateById(captor.capture());
        assertEquals(3, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("Ban host: canAcceptOrder becomes 0")
    void testBanHost_success() {
        Long hostId = 10L;
        Host host = new Host();
        host.setId(hostId);
        host.setCanAcceptOrder(1);

        when(hostService.getById(hostId)).thenReturn(host);
        when(hostService.updateById(any(Host.class))).thenReturn(true);

        Result<Void> result = commissionController.banHost(hostId);

        assertEquals("00000", result.getCode());

        ArgumentCaptor<Host> captor = ArgumentCaptor.forClass(Host.class);
        verify(hostService).updateById(captor.capture());
        assertEquals(0, captor.getValue().getCanAcceptOrder());
    }

    @Test
    @DisplayName("Unban host: fails with outstanding overdue commissions")
    void testUnbanHost_failsWithOverdue() {
        Long hostId = 10L;
        Host host = new Host();
        host.setId(hostId);
        host.setCanAcceptOrder(0);

        when(hostService.getById(hostId)).thenReturn(host);
        when(commissionOrderService.count(any(LambdaQueryWrapper.class))).thenReturn(2L);

        Result<Void> result = commissionController.unbanHost(hostId);

        assertNotEquals("00000", result.getCode());
        assertTrue(result.getMessage().contains("逾期"));
        verify(hostService, never()).updateById(argThat(h -> ((Host) h).getCanAcceptOrder() != null && ((Host) h).getCanAcceptOrder() == 1));
    }

    @Test
    @DisplayName("Unban host: succeeds when no overdue commissions")
    void testUnbanHost_success() {
        Long hostId = 10L;
        Host host = new Host();
        host.setId(hostId);
        host.setCanAcceptOrder(0);

        when(hostService.getById(hostId)).thenReturn(host);
        when(commissionOrderService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(hostService.updateById(any(Host.class))).thenReturn(true);

        Result<Void> result = commissionController.unbanHost(hostId);

        assertEquals("00000", result.getCode());

        ArgumentCaptor<Host> captor = ArgumentCaptor.forClass(Host.class);
        verify(hostService).updateById(captor.capture());
        assertEquals(1, captor.getValue().getCanAcceptOrder());
    }

    // ======================== 5. Withdrawal Tests ========================

    @Test
    @DisplayName("Withdraw: balance decreased, WithdrawalRequest created")
    void testWithdraw_success() {
        Long hostId = 10L;
        HostWallet wallet = buildWallet(hostId, new BigDecimal("5000.00"), new BigDecimal("1000.00"));

        securityUtilsMock.when(SecurityUtils::isHost).thenReturn(true);
        securityUtilsMock.when(SecurityUtils::getCurrentRefId).thenReturn(hostId);
        securityUtilsMock.when(SecurityUtils::getCurrentRealName).thenReturn("TestHost");

        when(hostWalletService.getOne(any(LambdaQueryWrapper.class))).thenReturn(wallet);
        when(withdrawalRequestService.save(any(WithdrawalRequest.class))).thenReturn(true);
        when(hostWalletService.updateById(any(HostWallet.class))).thenReturn(true);

        Map<String, Object> params = new HashMap<>();
        params.put("amount", "3000.00");
        params.put("accountType", "alipay");
        params.put("accountNo", "test@test.com");
        params.put("accountName", "TestHost");

        Result<Void> result = hostWalletController.withdraw(params);

        assertEquals("00000", result.getCode());

        // Balance should decrease by 3000
        assertEquals(0, new BigDecimal("2000.00").compareTo(wallet.getBalance()));

        // WithdrawalRequest should be created with status=1
        ArgumentCaptor<WithdrawalRequest> captor = ArgumentCaptor.forClass(WithdrawalRequest.class);
        verify(withdrawalRequestService).save(captor.capture());
        assertEquals(1, captor.getValue().getStatus());
        assertEquals(0, new BigDecimal("3000.00").compareTo(captor.getValue().getAmount()));
    }

    @Test
    @DisplayName("Withdraw: fails when amount exceeds available balance")
    void testWithdraw_insufficientBalance() {
        Long hostId = 10L;
        // balance=5000, frozen=1000 => available=4000
        HostWallet wallet = buildWallet(hostId, new BigDecimal("5000.00"), new BigDecimal("1000.00"));

        securityUtilsMock.when(SecurityUtils::isHost).thenReturn(true);
        securityUtilsMock.when(SecurityUtils::getCurrentRefId).thenReturn(hostId);

        when(hostWalletService.getOne(any(LambdaQueryWrapper.class))).thenReturn(wallet);

        Map<String, Object> params = new HashMap<>();
        params.put("amount", "4500.00"); // exceeds available 4000
        params.put("accountType", "alipay");
        params.put("accountNo", "test@test.com");
        params.put("accountName", "TestHost");

        Result<Void> result = hostWalletController.withdraw(params);

        assertNotEquals("00000", result.getCode());
        assertTrue(result.getMessage().contains("余额不足"));
        verify(withdrawalRequestService, never()).save(any());
    }

    @Test
    @DisplayName("Approve withdrawal: status becomes 2")
    void testApproveWithdrawal() {
        Long requestId = 1L;
        WithdrawalRequest wr = new WithdrawalRequest();
        wr.setId(requestId);
        wr.setHostId(10L);
        wr.setAmount(new BigDecimal("3000.00"));
        wr.setStatus(1);

        when(withdrawalRequestService.getById(requestId)).thenReturn(wr);
        when(withdrawalRequestService.updateById(any(WithdrawalRequest.class))).thenReturn(true);
        securityUtilsMock.when(SecurityUtils::getCurrentUsername).thenReturn("admin");

        Map<String, Object> params = new HashMap<>();
        params.put("action", "approve");

        Result<Void> result = hostWalletController.auditWithdrawal(requestId, params);

        assertEquals("00000", result.getCode());

        ArgumentCaptor<WithdrawalRequest> captor = ArgumentCaptor.forClass(WithdrawalRequest.class);
        verify(withdrawalRequestService).updateById(captor.capture());
        assertEquals(2, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("Reject withdrawal: status becomes 3, balance restored")
    void testRejectWithdrawal() {
        Long requestId = 1L;
        Long hostId = 10L;
        WithdrawalRequest wr = new WithdrawalRequest();
        wr.setId(requestId);
        wr.setHostId(hostId);
        wr.setAmount(new BigDecimal("3000.00"));
        wr.setStatus(1);

        HostWallet wallet = buildWallet(hostId, new BigDecimal("2000.00"), new BigDecimal("500.00"));

        when(withdrawalRequestService.getById(requestId)).thenReturn(wr);
        when(withdrawalRequestService.updateById(any(WithdrawalRequest.class))).thenReturn(true);
        when(hostWalletService.getOne(any(LambdaQueryWrapper.class))).thenReturn(wallet);
        when(hostWalletService.updateById(any(HostWallet.class))).thenReturn(true);
        securityUtilsMock.when(SecurityUtils::getCurrentUsername).thenReturn("admin");

        Map<String, Object> params = new HashMap<>();
        params.put("action", "reject");
        params.put("rejectReason", "Information incomplete");

        Result<Void> result = hostWalletController.auditWithdrawal(requestId, params);

        assertEquals("00000", result.getCode());

        // Verify status set to 3
        ArgumentCaptor<WithdrawalRequest> wrCaptor = ArgumentCaptor.forClass(WithdrawalRequest.class);
        verify(withdrawalRequestService).updateById(wrCaptor.capture());
        assertEquals(3, wrCaptor.getValue().getStatus());

        // Verify balance restored
        assertEquals(0, new BigDecimal("5000.00").compareTo(wallet.getBalance()));
    }
}
