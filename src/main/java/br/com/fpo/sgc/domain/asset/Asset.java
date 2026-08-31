package br.com.fpo.sgc.domain.asset;

import br.com.fpo.sgc.common.entity.BaseEntity;
import br.com.fpo.sgc.domain.location.sector.Sector;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tag_number", nullable = false, unique = true, length = 50)
    private String tagNumber;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String model;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssetStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;
}