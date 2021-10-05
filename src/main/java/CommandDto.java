import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CommandDto implements Serializable{
    private static final long serialVersionUID = -292657650076942888L;

    private Command command;
    private List<String> arguments;
}
