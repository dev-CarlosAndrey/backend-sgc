package br.com.fpo.sgc.domain.serviceorder;

import br.com.fpo.sgc.common.entity.BaseEntity;
import br.com.fpo.sgc.domain.asset.Asset;
import br.com.fpo.sgc.domain.location.sector.Sector;
import br.com.fpo.sgc.domain.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tb_service_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceOrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceOrderPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceOrderChannel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private User technician;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @Column(name = "solution_details", columnDefinition = "TEXT")
    private String solutionDetails;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "cost", precision = 12, scale = 2)
    private BigDecimal cost;

    // Nota de 1 a 5 dada pelo solicitante depois da conclusão; nula enquanto não avaliada.
    @Column(name = "rating")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer rating;

    @Column(name = "rated_at")
    private Instant ratedAt;
}
