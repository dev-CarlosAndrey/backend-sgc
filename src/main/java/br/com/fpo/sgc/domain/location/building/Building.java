package br.com.fpo.sgc.domain.location.building;

import br.com.fpo.sgc.domain.company.Company;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_buildings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 255)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}