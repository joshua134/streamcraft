package live.streamcraft.entity;

import org.hibernate.annotations.NaturalId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tbl_stream_category", indexes = {
	@Index(name = "idx_category_name", columnList = "name"),
    @Index(name = "idx_category_active", columnList = "active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class StreamCategory extends BaseEntity {
	@NaturalId
	@NotNull(message="Stream category name is required.")
    @Column(unique = true, nullable = false, length = 50)
    private String name;
    
    @Column(length = 200)
    private String description;
    
    @Column(length = 100)
    private String iconUrl;
    
    @Builder.Default
    private boolean active = true;
    
    private int displayOrder;
}
