package br.com.fpo.sgc.domain.serviceorder.statuslog;

import br.com.fpo.sgc.common.entity.BaseEntity;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrder;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_service_order_status_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceOrderStatusLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_order_id", nullable = false)
    private ServiceOrder serviceOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private ServiceOrderStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private ServiceOrderStatus newStatus;

    @Column(name = "changed_by", length = 150)
    private String changedBy;

    @Column(columnDefinition = "TEXT")
    private String note;
}
