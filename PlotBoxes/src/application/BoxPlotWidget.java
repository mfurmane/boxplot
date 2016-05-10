package application;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class BoxPlotWidget extends Group {
	private Pane pane;
	private List<BoxPlot> boxes;
	private double dist = 10;
	private double border = 40;
	private double y;
	private double refHigh;
	private double refWidth;
	private long updateAt;
	private BoxType widgetType;
	private double minVal;
	private double maxVal;
	private ExecutorService service;

	/**
	 * It returns created Pane
	 * @return - as said above
	 */
	public Pane getPane() {
		return pane;
	}
	
	/**
	 * without calling it application leaves some running threads
	 */
	public void clean() {
		service.shutdown();
	}

	/**
	 * It creates BoxPlotWidget - a widget that show data sets as boxes.
	 * @param x - x location of widget
	 * @param y - y location of widget
	 * @param width - widget width
	 * @param height - widget height
	 * @param updateAt - how often view should be refreshed
	 * @param widgetType - it specifies way of removing data from set; CLEAR_RANDOM - as name suggests; FIFO - first added, first fucked off
	 * @param maxVal - max of range which will be used in plot
	 * @param minVal - min of range which will be used in plot
	 */
	public BoxPlotWidget(double x, double y, double width, double height, long updateAt, BoxType widgetType,
			double maxVal, double minVal) {
		refHigh = height - 20;
		refWidth = width - border;
		pane = new Pane(this);
		pane.setPrefWidth(width);
		pane.setPrefHeight(height);
		pane.setLayoutX(x);
		pane.setLayoutY(y);
		pane.setStyle("-fx-background-color: yellow;-fx-border-color: #2e8b57;-fx-border-width: 1px;");
//		pane.setbo
		pane.getChildren().add(new Line(0,height-10,20, height-10));
		pane.getChildren().add(new Line(0,10,20, 10));
		pane.getChildren().add(new Line(5,10,5, height-10));
		double inter = refHigh/10;
		for (double i = 10+inter; i < height-10; i+=inter) pane.getChildren().add(new Line(0,i,10, i));
		Text mx = new Text(Double.toString(maxVal));
		mx.setLayoutX(0);
		mx.setLayoutY(height);
		Text mn = new Text(Double.toString(minVal));
		mn.setLayoutX(0);
		mn.setLayoutY(10);
		pane.getChildren().addAll(mx, mn);
		this.y = y - 10;
		this.updateAt = updateAt;
		this.widgetType = widgetType;
		this.maxVal = maxVal;
		this.minVal = minVal;
		boxes = new Vector<BoxPlot>();// BoxPlot(this, 20, 20, 10, 200);
		service = Executors.newFixedThreadPool((int)(refWidth/7));
	}

	/**
	 * This function creates new Box (data set) in widget
	 * @param maxSize - size of data set; if set will have more elements, it will remove some
	 * @return - id of created box (needed to add elements)
	 */
	public int addBox(int maxSize) {
		int id = boxes.size();
		double xpos = border;
		double width = refWidth / (id + 1);
		dist = 0.2 * width;
		width -= dist;
		if (width < 6) throw new RuntimeException("To many boxes");
		boxes.add(new BoxPlot(this, widgetType, xpos, y, width, refHigh, maxVal, minVal, id, maxSize, updateAt));
		for (BoxPlot boxPlot : boxes) {
			boxPlot.setWidth(width);
			boxPlot.setXpos(xpos + boxPlot.getId() * (width + dist));
		}
		return id;
	}

	/**
	 * This function adds single data element (dat) to box with specified id
	 * @param id - id of destination box
	 * @param dat - data element to insert
	 */
	public void addData(int id, double dat) {
		boxes.get(id).addData(dat);
	}

	/**
	 * This function adds multiple data elements (dat) to box with specified id
	 * @param id - id of destination box
	 * @param dat - list of data to insert
	 */
	public void addData(int id, List<Double> dat) {
		boxes.get(id).addData(dat);
	}

	private class BoxPlot {
		public int getId() {
			return id;
		}

		private Random rand = new Random();
		private int id;
		private int maxsize;
		private List<Double> data = new Vector<>();
		private List<Double> preData = new Vector<>();
		private Rectangle rectUp = new Rectangle();
		private Rectangle rectDown = new Rectangle();
		private Line up = new Line();
		private Line down = new Line();
		private Line vert = new Line();
		private double width = 100d;
		private double height = 450d;
		private double xpos = 50d;
		private double ypos = 50d;
		private double maxVal = 400d;
		private double minVal = 0d;
		private BoxType type;

		public BoxPlot(Group group, BoxType type, double xpos, double ypos, double width, double height, double maxVal,
				double minVal, int id, int maxsize, long updateAt) {
			group.getChildren().add(vert);
			group.getChildren().add(rectUp);
			group.getChildren().add(rectDown);
			group.getChildren().add(up);
			group.getChildren().add(down);
			this.xpos = xpos;
			this.ypos = ypos;
			this.width = width;
			this.height = height;
			this.id = id;
			this.maxsize = maxsize;
			this.type = type;
			this.maxVal = maxVal;
			this.minVal = minVal;
			service.execute(new Runnable() {
				@Override
				public void run() {
					while (true) {
						try {
							Thread.sleep(updateAt);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						updateView();
					}
				}
			});
		}
		
		private double median(List<Double> data) {
			int middle = data.size() / 2;
			if (data.size() > 0)
				if (data.size() % 2 == 1) {
					return data.get(middle);
				} else {
					return (data.get(middle - 1) + data.get(middle)) / 2.0;
				}
			else
				return 0;
		}

		private Vector<Double> getHalfs(double med) {
			List<Double> half1 = new Vector<>();
			List<Double> half2 = new Vector<>();
			synchronized (data) {
				for (Double doub : data) {
					if (doub < med)
						half1.add(doub);
					else
						half2.add(doub);
				}

			}
			Vector<Double> result = new Vector<Double>();
			result.add(median(half1));
			result.add(median(half2));
			return result;
		}

		private void updateView() {
			double med = 0, min = 0, max = 0;
			switch (type) {
			case CLEAR_RANDOM:
				synchronized (preData) {
					data = preData;
					if (data.size() > 0) {
						Collections.sort(data);
						med = median(data);
						min = data.get(0);
						max = data.get(data.size() - 1);
					}
				}
				break;
			case FIFO:
				data.clear();
				synchronized (preData) {
					data.addAll(preData);
				}
				if (data.size() > 0) {
					Collections.sort(data);
					med = median(data);
					min = data.get(0);
					max = data.get(data.size() - 1);
				}
			}
			Vector<Double> middle = getHalfs(med);
			med = (med - minVal) / (maxVal - minVal) * height + ypos;
			min = (min - minVal) / (maxVal - minVal) * height + ypos;
			max = (max - minVal) / (maxVal - minVal) * height + ypos;
			middle.set(0, (middle.get(0) - minVal) / (maxVal - minVal) * height + ypos);
			middle.set(1, (middle.get(1) - minVal) / (maxVal - minVal) * height + ypos);

			rectUp.setX(xpos);
			rectUp.setY(middle.get(0));
			rectUp.setWidth(width);
			rectUp.setHeight(med - middle.get(0));
			rectUp.setFill(Paint.valueOf("red"));
			rectDown.setX(xpos);
			rectDown.setY(med);
			rectDown.setWidth(width);
			rectDown.setHeight(middle.get(1) - med);
			rectDown.setFill(Paint.valueOf("black"));
			up.setStartX(xpos);
			up.setEndX(xpos + width);
			down.setStartX(xpos);
			down.setEndX(xpos + width);
			up.setStartY(min);
			up.setEndY(min);
			down.setStartY(max);
			down.setEndY(max);
			vert.setEndY(max);
			vert.setStartY(min);
			vert.setStartX(xpos + width / 2);
			vert.setEndX(xpos + width / 2);
		}

		public void addData(double dat) {
			switch (type) {
			case CLEAR_RANDOM:
				addDataOnHurra(dat);
				break;
			case FIFO:
				addDataCarefully(dat);
			}
		}

		private void addDataCarefully(double dat) {
			synchronized (preData) {
				preData.add(dat);
				if (preData.size() > maxsize)
					preData.remove(0);
			}
		}

		private void addDataOnHurra(double dat) {
			synchronized (preData) {
				preData.add(dat);
				if (preData.size() > maxsize)
					preData.remove(rand.nextInt(preData.size()));
			}
		}

		public void addData(List<Double> dat) {
			switch (type) {
			case CLEAR_RANDOM:
				addDataOnHurra(dat);
				break;
			case FIFO:
				addDataCarefully(dat);
			}
		}

		private void addDataCarefully(List<Double> dat) {
			synchronized (preData) {
				preData.addAll(dat);
				while (preData.size() > maxsize)
					preData.remove(0);
			}
		}

		private void addDataOnHurra(List<Double> dat) {
			synchronized (preData) {
				preData.addAll(dat);
				while (preData.size() > maxsize)
					preData.remove(rand.nextInt(preData.size()));
			}
		}

		public void setWidth(double width) {
			this.width = width;
		}

		public void setXpos(double xpos) {
			this.xpos = xpos;
		}
	}
}
