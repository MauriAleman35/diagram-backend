import javax.persistence.*;
import lombok.*;
import java.util.Set;

@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Entity
@Table(name = "Erick Gei", schema = "public")
public class Erick Gei {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    public Erick Gei(String name) {
        super();
        this.name = name;
    }
}
