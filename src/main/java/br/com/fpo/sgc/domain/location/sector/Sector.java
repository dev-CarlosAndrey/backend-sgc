package br.com.fpo.sgc.domain.location.sector;

import br.com.fpo.sgc.common.entity.BaseEntity;
import br.com.fpo.sgc.domain.employee.Employee;
import br.com.fpo.sgc.domain.location.building.Building;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_sectors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sector extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_employee_id")
    private Employee manager;
}