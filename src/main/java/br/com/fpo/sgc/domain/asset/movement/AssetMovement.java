package br.com.fpo.sgc.domain.asset.movement;

import br.com.fpo.sgc.common.entity.BaseEntity;
import br.com.fpo.sgc.domain.asset.Asset;
import br.com.fpo.sgc.domain.location.sector.Sector;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "tb_asset_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetMovement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_sector_id", nullable = false)
    private Sector originSector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_sector_id", nullable = false)
    private Sector destinationSector;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Builder.Default
    @Column(name = "movement_date", nullable = false)
    private Instant movementDate = Instant.now();
}