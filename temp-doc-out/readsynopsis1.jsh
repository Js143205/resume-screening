import org.apache.tika.Tika;
import java.nio.file.*;
var tika = new Tika();
var text = tika.parseToString(Files.newInputStream(Path.of("C:/Users/jagde/Downloads/project/Synopsis_1_Template_GNDU_MCA.pdf")));
System.out.println(text);
/exit
