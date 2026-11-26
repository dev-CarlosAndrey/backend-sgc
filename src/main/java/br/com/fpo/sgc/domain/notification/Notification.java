package br.com.fpo.sgc.domain.notification;

import br.com.fpo.sgc.common.entity.BaseEntity;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrder;
import br.com.fpo.sgc.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_order_id", nullable = false)
    private ServiceOrder serviceOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 20)
    private NotificationStatus deliveryStatus;

    @Column(nullable = false)
    private Integer attempt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    // Id da mensagem no provedor (wamid), para casar o status "failed" que a Meta envia depois por webhook.
    @Column(name = "provider_message_id", length = 128)
    private String providerMessageId;

    // Assunto e texto enviados pelo WhatsApp, guardados para reenviar pelo e-mail se a Meta informar falha.
    @Column(length = 255)
    private String subject;

    @Column(name = "message_body", length = 2000)
    private String messageBody;
}
