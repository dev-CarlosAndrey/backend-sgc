package br.com.fpo.sgc.domain.whatsapp;

import br.com.fpo.sgc.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/** Mensagem recebida no webhook já processada, registrada pelo id da Meta para não processá-la duas vezes (RNF11). */
@Entity
@Table(name = "tb_whatsapp_inbound_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppInboundMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true, length = 128)
    private String messageId;

    @Column(name = "from_phone", length = 20)
    private String fromPhone;
}
