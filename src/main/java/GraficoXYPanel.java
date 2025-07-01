/*import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class GraficoXYPanel extends JPanel {
    private final XYSeries serie;
    private final XYSeries baseline;
    private final XYSeriesCollection dataset;

    public GraficoXYPanel(List<Double> xList, List<Double> yList) {
        this.serie = new XYSeries("curve");
        this.baseline = new XYSeries("baseline");

        for (int i = 0; i < xList.size(); i++) {
            serie.add(xList.get(i), yList.get(i));
            baseline.add(xList.get(i), (Number) 0);
        }

        dataset = new XYSeriesCollection();
        dataset.addSeries(serie);
        dataset.addSeries(baseline);

        JFreeChart chart = ChartFactory.createXYLineChart(null, null, null, dataset);
        XYPlot plot = chart.getXYPlot();

        Color grigioScuro = new Color(0x3b3b3b);
        chart.setBackgroundPaint(Color.BLACK);
        plot.setBackgroundPaint(grigioScuro);

        plot.getDomainAxis().setTickLabelPaint(Color.WHITE);
        plot.getRangeAxis().setTickLabelPaint(Color.WHITE);
        plot.getDomainAxis().setTickMarkPaint(Color.WHITE);
        plot.getRangeAxis().setTickMarkPaint(Color.WHITE);

        Stroke asseSpesso = new BasicStroke(2f);
        plot.getDomainAxis().setAxisLinePaint(Color.WHITE);
        plot.getRangeAxis().setAxisLinePaint(Color.WHITE);
        plot.getDomainAxis().setAxisLineStroke(asseSpesso);
        plot.getRangeAxis().setAxisLineStroke(asseSpesso);

        plot.setDomainGridlinePaint(new Color(200, 200, 200));
        plot.setRangeGridlinePaint(new Color(200, 200, 200));
        Stroke grigliaContinua = new BasicStroke(0.25f);
        plot.setDomainGridlineStroke(grigliaContinua);
        plot.setRangeGridlineStroke(grigliaContinua);

        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);

        chart.removeLegend();

        XYDifferenceRenderer areaRenderer = new XYDifferenceRenderer(
                new Color(0, 255, 0, 40), Color.WHITE, false
        );
        areaRenderer.setSeriesOutlinePaint(0, null);
        areaRenderer.setSeriesOutlineStroke(0, new BasicStroke(0f));
        areaRenderer.setSeriesOutlinePaint(1, new Color(0, 0, 0, 0));
        areaRenderer.setSeriesOutlineStroke(1, new BasicStroke(0f));
        areaRenderer.setRoundXCoordinates(true);
        plot.setRenderer(0, areaRenderer);

        XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer(true, false);
        lineRenderer.setSeriesPaint(0, Color.BLUE);
        lineRenderer.setSeriesStroke(0, new BasicStroke(1.0f));
        plot.setRenderer(1, lineRenderer);

        plot.setDataset(0, dataset);
        plot.setDataset(1, dataset);

        setLayout(new BorderLayout());
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(700, 475));
        add(chartPanel, BorderLayout.CENTER);
    }

    public void aggiornaDati(List<Double> xList, List<Double> yList) {
        serie.clear();
        baseline.clear();
        for (int i = 0; i < xList.size(); i++) {
            serie.add(xList.get(i), yList.get(i));
            baseline.add(xList.get(i), (Number)0);
        }
    }
}*/
