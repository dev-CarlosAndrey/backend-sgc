package br.com.fpo.sgc.domain.company;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String companyName;

    @Column(nullable = false, unique = true, length = 18)
    private String cnpj;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 255)
    private String logoUrl;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}