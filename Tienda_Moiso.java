import javax.persistence.*;
import lombok.*;
import java.util.Set;

@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Entity
@Table(name = "Tienda_Moiso", schema = "public")
public class Tienda_Moiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer tiendaId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer moisoId;

    public Tienda_Moiso() {
        super();

    }
}
