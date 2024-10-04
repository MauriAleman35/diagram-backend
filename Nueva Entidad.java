import javax.persistence.*;
import lombok.*;
import java.util.Set;

@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Entity
@Table(name = "Nueva Entidad", schema = "public")
public class Nueva Entidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false)
    private Integer name;

    public Nueva Entidad(Integer name) {
        super();
        this.name = name;
    }
}
