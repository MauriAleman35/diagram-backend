import javax.persistence.*;
import lombok.*;

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

    public Tienda(String nombre, String direccion, String ciudad) {
        super();
        this.nombre = nombre;
        this.direccion = direccion;
        this.ciudad = ciudad;
    }
}
