// DatiMisura.java
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DatiMisura {
    public final List<Double> xList = new ArrayList<>();
    public final List<Double> yList;
    public final List<Integer> valoriCRI;
    public final Map<String, Double> datiValori;

    public DatiMisura( List<Double> yList, List<Integer> valoriCRI, Map<String, Double> datiValori) {
        for (int j = 0; j < 471; j++) {
            double x = 360.0 + j;
            xList.add(Double.valueOf(x));
        }
        this.yList = yList;
        this.valoriCRI = valoriCRI;
        this.datiValori = datiValori;
    }
}