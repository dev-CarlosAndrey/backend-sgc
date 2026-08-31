package br.com.fpo.sgc.domain.brand;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_brands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}