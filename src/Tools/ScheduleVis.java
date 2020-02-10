package Tools;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class ScheduleVis extends JPanel {

	private int nWeeks; 
	private int boundX; 
	private int heightpWeek; 
	private int widthpDay; 
	private int xBuffer; 
	private String[] duties; 
	private int heightDutyBlock; 
	private HashMap<Integer, Duty> dutyNToDuty; 
	private boolean basic; 
	private int yBuffer; 


	public static void main(String [] args) {
		JFrame frame = new JFrame("CrewRoster"); 
		int nWeeks = 2; 
		int heightpWeek = 50; 
		int widthpDay = 100; 
		int xLegend = 100; 
		int yBuffer = 50; 
		String[] duties = new String[] {"V", "ATV",  "L", "V", "M", "L", "D","R", "ATV",  "L", "V", "W", "D", "D" }; 
		JPanel canvas = new ScheduleVis(nWeeks, heightpWeek, widthpDay, xLegend, yBuffer, duties);
		canvas.setSize(7*widthpDay + xLegend, nWeeks*heightpWeek );
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(canvas);
		frame.pack();
		frame.setVisible(true);
	}


	public ScheduleVis(int nWeeks, int heightpWeek, int widthpDay, int xLegend, int yBuffer, String[] duties) {
		this.nWeeks = nWeeks; 
		this.boundX = 7*100; 
		this.heightpWeek = heightpWeek; 
		this.widthpDay = widthpDay; 
		this.xBuffer = xLegend; 
		this.duties = duties; 
		this.heightDutyBlock = 20; 
		this.basic = true; 
		this.yBuffer = yBuffer; 
	}

	public ScheduleVis(int nWeeks, int heightpWeek, int widthpDay, int xLegend, int yBuffer,  String[] duties, HashMap<Integer, Duty>dutyNToDuty) {
		this.nWeeks = nWeeks; 
		this.boundX = 7*100;  
		this.heightpWeek = heightpWeek; 
		this.widthpDay = widthpDay; 
		this.xBuffer = xLegend; 
		this.duties = duties; 
		this.heightDutyBlock = 20; 
		this.basic = false; 
		this.dutyNToDuty = dutyNToDuty; 
		this.yBuffer = yBuffer; 
	}

	public ScheduleVis(String[] duties, String contractGroupNr ) {
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				int nWeeks = duties.length/7; 
				System.out.println(nWeeks); 
				int heightpWeek = 50; 
				int widthpDay = 100; 
				int xLegend = 100; 
				int yBuffer = 50; 
				JPanel canvas = new ScheduleVis(nWeeks, heightpWeek, widthpDay, xLegend, yBuffer, duties);
				canvas.setPreferredSize(new Dimension(7*widthpDay + xLegend + 10, yBuffer + Math.max(15*heightpWeek ,nWeeks*heightpWeek) ));
				JScrollPane scrollPane = new JScrollPane(canvas); 
				scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
				JFrame frame = new JFrame("CrewRoster "+ contractGroupNr); 
				frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setVisible(true);
				frame.setLocationRelativeTo(null);
			}
		});
	}

	public void paint(Graphics g) {
		int fontSize = 15; 
		g.setFont(new Font("TimesRoman", Font.PLAIN, fontSize)); 

		g.drawString("Sunday", xBuffer,  yBuffer);
		g.drawString("Monday", xBuffer + 1*widthpDay,  yBuffer);
		g.drawString("Tuesday", xBuffer + 2*widthpDay,  yBuffer);
		g.drawString("Wednesday", xBuffer + 3*widthpDay,  yBuffer);
		g.drawString("Thursday", xBuffer + 4*widthpDay,  yBuffer);
		g.drawString("Friday", xBuffer + 5*widthpDay,  yBuffer);
		g.drawString("Saturday", xBuffer + 6*widthpDay,  yBuffer);

		for(int i = 0; i< this.nWeeks; i++) {
			g.drawLine(xBuffer,yBuffer+  heightpWeek/2 + i* heightpWeek, boundX + xBuffer, yBuffer+ heightpWeek/2 + i* heightpWeek);
			g.drawString("Week " + i, 10, yBuffer + i* heightpWeek + heightpWeek/2 + fontSize/2);
			for(int j = 1; j<=7+1; j++) {
				//Draw vertical day lines
				g.drawLine((j-1)*this.widthpDay + xBuffer,yBuffer+  heightpWeek/2 + i* heightpWeek - 15, (j-1)*this.widthpDay + xBuffer, yBuffer+ heightpWeek/2 + i* heightpWeek + 15);
			}

			for(int j = 0; j<7; j++) {
				Color c = Color.LIGHT_GRAY; 
				int startTime = 0; 
				int endTime = 0 ; 
				int minpDay = 24*60; 
				if(basic) {
					//Check for each day what duty it is 
					if(duties[j + i*7].equals("V")) {
						startTime = 4*60 + 30; 
						endTime = 16*60 + 30; 
					}else if(duties[j + i*7].equals("D")) {
						startTime = 10*60; 
						endTime = 18*60 + 15; 
					}else if(duties[j+ i*7].equals("L")) {
						startTime = 13*60; 
						endTime = 25*60 ; 
					} else if(duties[j+ i*7].equals("GM")||duties[j+ i*7].equals("G")) {
						startTime = 5*60 + 30; 
						endTime = 18*60 + 15; 
					} else if(duties[j + i*7].equals("ATV")) {
						c = new Color(255,192,203); 
						startTime = 0; 
						endTime = 24*60; 
					}else if(duties[j + i*7].equals("Rest")) {
						c = new Color(152,251,152); 
						startTime = 0; 
						endTime = 24*60; 
					}
					else if(duties[j + i*7].equals("W")) {
						startTime = 4*60; 
						endTime = 25*60; 
					}else if(duties[j + i*7].equals("RV")) {
						startTime = 4*60 +30 ; 
						endTime = 16*60 + 30 ; 
					}else if(duties[j + i*7].equals("RG")) {
						startTime = 5*60 + 30 ; 
						endTime = 18*60 + 30; 
					}else if(duties[j + i*7].equals("RD")) {
						startTime = 8*60; 
						endTime = 20*60; 
					}else if(duties[j + i*7].equals("RL")) {
						startTime = 13*60; 
						endTime = 25*60; 
					}else if(duties[j+ i*7].equals("P")) {
						c = Color.yellow; 
						startTime = 0; 
						endTime = 24*60 ; 
					}
				}
				else {
					Duty duty = this.dutyNToDuty.get(Integer.parseInt(duties[j + i*7])); 
					startTime = duty.getStartTime(); 
					endTime = duty.getEndTime(); 
				}
				int duration = endTime - startTime; 
				g.drawRect((startTime*widthpDay/minpDay) + xBuffer + j*widthpDay,yBuffer+  heightpWeek/2 + i* heightpWeek - heightDutyBlock/2 , duration*widthpDay/minpDay, heightDutyBlock);
				g.setColor(c);
				g.fillRect((startTime*widthpDay/minpDay) + xBuffer + j*widthpDay, yBuffer+ heightpWeek/2 + i* heightpWeek - heightDutyBlock/2 , duration*widthpDay/minpDay, heightDutyBlock);
				
				//Write type of duty 
				g.setColor(Color.BLACK);
				int nLetters = duties[j + i*7].length(); 
				int xString = (startTime*widthpDay/minpDay) + xBuffer + j*widthpDay +  duration*widthpDay/(2*minpDay) - fontSize*nLetters/2; 
				int yString = heightpWeek/2 + i* heightpWeek - heightDutyBlock/2 + fontSize ; 
				g.drawString(duties[j + i*7],xString ,yBuffer+  yString);
			}	
		}
	}
}