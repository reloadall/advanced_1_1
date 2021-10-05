import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Client {

    public static void main(String[] args) {
        var dto = new CommandDto();
        dto.setCommand(Command.SUM);
        dto.setArguments(List.of(String.valueOf(111111111111L)));
        try {
            var channel = SocketChannel.open(new InetSocketAddress(InetAddress.getLocalHost(), 9998));
            var buffer = createBytes(dto);
            channel.write(buffer);
            buffer.clear();
            channel.read(buffer);
            var response = new String(buffer.array(),
                    buffer.arrayOffset(),
                    buffer.arrayOffset()+buffer.position(),
                    StandardCharsets.UTF_8);
            System.out.println("response=" + response);
            buffer.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ByteBuffer createBytes(CommandDto dto) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ObjectOutputStream out;
            out = new ObjectOutputStream(bos);
            out.writeObject(dto);
            out.flush();
            return ByteBuffer.wrap(bos.toByteArray());
        }
    }

}
