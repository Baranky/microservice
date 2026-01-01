package com.example.demo.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.PaymentResponse;
import com.example.demo.entity.Payment;
import com.example.demo.enums.PaymentMethod;
import com.example.demo.enums.PaymentStatus;
import com.example.demo.event.PaymentConfirmedEvent;
import com.example.demo.event.PaymentFailedEvent;
import com.example.demo.event.StockReservedEvent;
import com.example.demo.repository.PaymentRepository;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final int MAX_RETRY_COUNT = 3;

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, PaymentConfirmedEvent> paymentConfirmedKafkaTemplate;
    private final KafkaTemplate<String, PaymentFailedEvent> paymentFailedKafkaTemplate;

    public PaymentService(PaymentRepository paymentRepository,
            KafkaTemplate<String, PaymentConfirmedEvent> paymentConfirmedKafkaTemplate,
            KafkaTemplate<String, PaymentFailedEvent> paymentFailedKafkaTemplate) {
        this.paymentRepository = paymentRepository;
        this.paymentConfirmedKafkaTemplate = paymentConfirmedKafkaTemplate;
        this.paymentFailedKafkaTemplate = paymentFailedKafkaTemplate;
    }


    public Payment createPendingPayment(StockReservedEvent stockReservedEvent) {
        log.info("Payment kaydı oluşturuluyor (manuel ödeme bekleniyor): orderId={}, productId={}, quantity={}, totalPrice={}, customerEmail={}",
                stockReservedEvent.orderId(), stockReservedEvent.productId(), stockReservedEvent.quantity(),
                stockReservedEvent.totalPrice(), stockReservedEvent.customerEmail());

        Payment payment = new Payment();
        payment.setOrderId(String.valueOf(stockReservedEvent.orderId()));
        payment.setUserId(stockReservedEvent.customerEmail());
        payment.setAmount(stockReservedEvent.totalPrice());
        payment.setCurrency("TRY");
        payment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setProductId(stockReservedEvent.productId());
        payment.setQuantity(stockReservedEvent.quantity());
        payment.setRetryCount(0);
        payment.setExternalTransactionId(UUID.randomUUID().toString());

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment kaydı oluşturuldu: paymentId={}, orderId={}, status=PENDING, amount={}",
                savedPayment.getId(), savedPayment.getOrderId(), savedPayment.getAmount());

        return savedPayment;
    }

    public Payment processManualPayment(String paymentId) {
        log.info("Manuel ödeme işlemi başlatılıyor: paymentId={}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Ödeme bulunamadı: " + paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Ödeme zaten işlenmiş: status=" + payment.getStatus());
        }

        PaymentStatus paymentStatus = processPayment(payment.getAmount());

        payment.setStatus(paymentStatus);
        payment.setRetryCount(0);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Manuel ödeme işlemi tamamlandı: paymentId={}, orderId={}, status={}",
                savedPayment.getId(), savedPayment.getOrderId(), savedPayment.getStatus());

        publishPaymentEventForManualPayment(savedPayment);

        return savedPayment;
    }


    private void publishPaymentEventForManualPayment(Payment payment) {
        Long orderId = Long.parseLong(payment.getOrderId());

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            PaymentConfirmedEvent paymentConfirmedEvent = new PaymentConfirmedEvent(
                    orderId,
                    payment.getId(),
                    payment.getAmount(),
                    payment.getStatus().name(),
                    payment.getUserId()
            );

            paymentConfirmedKafkaTemplate.send("payment-confirmed", paymentConfirmedEvent);
            log.info("Payment confirmed event gönderildi: orderId={}, paymentId={}, status=SUCCESS",
                    orderId, payment.getId());
        } else {
            String failureReason = payment.getFailureReason() != null ? payment.getFailureReason() : "Ödeme işlemi başarısız oldu";

            if (payment.getProductId() != null && payment.getQuantity() != null) {
                PaymentFailedEvent paymentFailedEvent = new PaymentFailedEvent(
                        orderId,
                        payment.getProductId(),
                        payment.getQuantity(),
                        payment.getId(),
                        failureReason
                );

                paymentFailedKafkaTemplate.send("payment-failed", paymentFailedEvent);
                log.info("Payment failed event gönderildi: orderId={}, paymentId={}, reason={}",
                        orderId, payment.getId(), failureReason);
            } else {
                log.warn("Ödeme başarısız, ancak productId ve quantity bilgisi yok: paymentId={}, orderId={}",
                        payment.getId(), orderId);
            }
        }
    }




    private PaymentStatus processPayment(java.math.BigDecimal amount) {
        log.info("Ödeme başarıyla tamamlandı: amount={}", amount);
        return PaymentStatus.SUCCESS;
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    public PaymentResponse getPaymentById(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Ödeme bulunamadı: " + paymentId));
        return toPaymentResponse(payment);
    }

    public PaymentResponse getPaymentByOrderId(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Ödeme bulunamadı: orderId=" + orderId));
        return toPaymentResponse(payment);
    }

    public List<PaymentResponse> getFailedPaymentsForRetry() {
        return paymentRepository.findByStatusAndRetryCountLessThan(PaymentStatus.FAILED, MAX_RETRY_COUNT).stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getPendingPayments() {
        return paymentRepository.findByStatus(PaymentStatus.PENDING).stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    public Payment rejectPayment(String paymentId) {
        log.info("Ödeme reddediliyor: paymentId={}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Ödeme bulunamadı: " + paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Sadece PENDING status'lu ödemeler reddedilebilir: status=" + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason("Ödeme reddedildi");
        payment.setRetryCount(0);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Ödeme reddedildi: paymentId={}, orderId={}, status=FAILED",
                savedPayment.getId(), savedPayment.getOrderId());


        publishPaymentEventForManualPayment(savedPayment);

        return savedPayment;
    }

    public PaymentResponse processManualPaymentResponse(String paymentId) {
        Payment payment = processManualPayment(paymentId);
        return toPaymentResponse(payment);
    }


    public PaymentResponse rejectPaymentResponse(String paymentId) {
        Payment payment = rejectPayment(paymentId);
        return toPaymentResponse(payment);
    }



    private PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getMaskedCardNumber(),
                payment.getCardHolderName(),
                payment.getCreatedAt()
        );
    }
}
