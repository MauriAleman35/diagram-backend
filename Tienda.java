import javax.persistence.*;
import lombok.*;
import java.util.Set;

@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Entity
@Table(name = "Tienda", schema = "public")
public class Tienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "direccion", nullable = false)
    private String direccion;

    @Column(name = "ciudad", nullable = false)
    private String ciudad;

    @ManyToMany
    @JoinTable(name = "tienda_moiso gei",
        joinColumns = @JoinColumn(name = "tienda_id"),
        inverseJoinColumns = @JoinColumn(name = "moiso gei_id"))
    private Set<Moiso gei> moiso gei;

    public Tienda(String nombre, String direccion, String ciudad) {
        super();
        this.nombre = nombre;
        this.direccion = direccion;
        this.ciudad = ciudad;
    }
}
