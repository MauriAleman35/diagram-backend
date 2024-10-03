import javax.persistence.*;
import lombok.*;

@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Entity
@Table(name = "sdfsdfsd", schema = "public")
public class sdfsdfsd {

    @Column(name = "sdf", nullable = false)
    private Integer sdf;

    @Column(name = "sdfsf", nullable = false)
    private Integer sdfsf;

    public sdfsdfsd(Integer sdf, Integer sdfsf) {
        super();
        this.sdf = sdf;
        this.sdfsf = sdfsf;
    }
}
