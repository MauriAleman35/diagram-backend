import javax.persistence.*;
import lombok.*;
import java.util.Set;

@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Entity
@Table(name = "Moiso gei", schema = "public")
public class Moiso gei {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "contraseña", nullable = false)
    private String contraseña;

    @Column(name = "email", nullable = false)
    private String email;

    public Moiso gei(String nombre, String contraseña, String email) {
        super();
        this.nombre = nombre;
        this.contraseña = contraseña;
        this.email = email;
    }
}
