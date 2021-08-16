package application;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SpaceInvaders extends Application{//Aqui se crea la clase SpaceInvaders, se usa el prefijo extends para poder agrupar todos los metodos y llamarlos de una sola vez al final del codigo
	
	//variables que utilizaremos para SpaceInvaders
	private static final Random RAND = new Random();
	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;
	private static final int PLAYER_SIZE = 60;
	static final Image PLAYER_IMG = new Image("file:C:/Users/carlo/Desktop/SInvaders/images/player.png"); 
	static final Image EXPLOSION_IMG = new Image("file:C:/Users/carlo/Desktop/SInvaders/images/explosion.png");
	static final int EXPLOSION_W = 128;
	static final int EXPLOSION_ROWS = 3;
	static final int EXPLOSION_COL = 3;
	static final int EXPLOSION_H = 128;
	static final int EXPLOSION_STEPS = 15;
	
	static final Image BOMBS_IMG[] = {
			new Image("file:C:/Users/carlo/Desktop/SInvaders/images/teach.png"),
			new Image("file:C:/Users/carlo/Desktop/SInvaders/images/kast.png"),
			new Image("file:C:/Users/carlo/Desktop/SInvaders/images/trump.png"),
			new Image("file:C:/Users/carlo/Desktop/SInvaders/images/darcy.png"),
			new Image("file:C:/Users/carlo/Desktop/SInvaders/images/vaca.png"),
			new Image("file:C:/Users/carlo/Desktop/SInvaders/images/6.png"),
			new Image("file:C:/Users/carlo/Desktop/SInvaders/images/7.png"),
			new Image("file:C:/Users/carlo/Desktop/SInvaders/images/8.png"),
			new Image("file:C:/Users/carlo/Desktop/SInvaders/images/9.png"),
			new Image("file:C:/Users/carlo/Desktop/SInvaders/images/10.png"),
	};
	
	//se fija maxima cantidad de bombas en pantalla , se establece que el maximo de disparos disponibles sea el doble de la cantidad de bombas para que siempre sea posible destruir todas las bombas
	final int MAX_BOMBS = 15,  MAX_SHOTS = MAX_BOMBS * 2;
	boolean gameOver = false;
	private GraphicsContext gc;
	
	Rocket player;
	List<Shot> shots;
	List<Universe> univ;
	List<Bomb> Bombs;
	
	private double mouseX;
	private int score;

	//Aqui se crea la interfaz inicial del juego
	public void start(Stage stage) throws Exception {
		Canvas canvas = new Canvas(WIDTH, HEIGHT);	
		gc = canvas.getGraphicsContext2D();
		Timeline timeline = new Timeline(new KeyFrame(Duration.millis(100), e -> run(gc)));
		timeline.setCycleCount(Timeline.INDEFINITE);
		timeline.play();
		canvas.setCursor(Cursor.MOVE);//hacemos que el usuario pueda mover la nave con el mouse 
		canvas.setOnMouseMoved(e -> mouseX = e.getX());
		canvas.setOnMouseClicked(e -> {
			if(shots.size() < MAX_SHOTS) shots.add(player.shoot());
			if(gameOver) { 
				gameOver = false;
				setup();
			}
		});
		setup();
		stage.setScene(new Scene(new StackPane(canvas)));
		stage.setTitle("Space Invaders");
		stage.show();
		
	}

	//configuraciones principales
	private void setup() {
		univ = new ArrayList<>();//se crea el mapa en el que se monta el juego
		shots = new ArrayList<>();//se crea el metodo disparo
		Bombs = new ArrayList<>();//se crean los enemigos
		player = new Rocket(WIDTH / 2, HEIGHT - PLAYER_SIZE, PLAYER_SIZE, PLAYER_IMG);//se crea el jugador usuario
		score = 0;//se crea atributo puntaje
		IntStream.range(0, MAX_BOMBS).mapToObj(i -> this.newBomb()).forEach(Bombs::add);
	}
	
	//se inicializa la interfaz del juego
	private void run(GraphicsContext gc) {
		gc.setFill(Color.grayRgb(20));
		gc.fillRect(0, 0, WIDTH, HEIGHT);
		gc.setTextAlign(TextAlignment.CENTER);
		gc.setFont(Font.font(20));
		gc.setFill(Color.WHITE);
		gc.fillText("Score: " + score, 60,  20);
	
		
		if(gameOver) {//se crea la pantalla de game over
			gc.setFont(Font.font(35));
			gc.setFill(Color.PURPLE);
			gc.fillText("Game Over \n tu puntaje es: " + score + " \n Click para jugar de nuevo", WIDTH / 2, HEIGHT /2.5);
		//	return;
		}
		univ.forEach(Universe::draw);
	
		player.update();//actualizacion de posicion
		player.draw();//inicializacion del usuario
		player.posX = (int) mouseX;//movimiento del usuario en direccion horizontal
		
		Bombs.stream().peek(Rocket::update).peek(Rocket::draw).forEach(e -> {//Si el usuario colisiona con una bomba, se le da da el atributo explode
			if(player.colide(e) && !player.exploding) {
				player.explode();
			}
		});
		
		
		for (int i = shots.size() - 1; i >=0 ; i--) {//se le define como seran los disparos
			Shot shot = shots.get(i);
			if(shot.posY < 0 || shot.toRemove)  { 
				shots.remove(i);
				continue;
			}
			shot.update();
			shot.draw();
			for (Bomb bomb : Bombs) {
				if(shot.colide(bomb) && !bomb.exploding) {
					score++;
					bomb.explode();
					shot.toRemove = true;
				}
			}
		}
		
		for (int i = Bombs.size() - 1; i >= 0; i--){//se da tamano a las bombas
			if(Bombs.get(i).destroyed)  {
				Bombs.set(i, newBomb());
			}
		}
	
		gameOver = player.destroyed;//se extablece condicion de game over
		if(RAND.nextInt(10) > 2) {
			univ.add(new Universe());
		}
		for (int i = 0; i < univ.size(); i++) {
			if(univ.get(i).posY > HEIGHT)
				univ.remove(i);
		}
	}

	//Atributos de jugador
	public class Rocket {

		int posX, posY, size;
		boolean exploding, destroyed;
		Image img;
		int explosionStep = 0;
		
		public Rocket(int posX, int posY, int size,  Image image) {//se le dan atributos de posision, se le da un tamano e icono al usuario
			this.posX = posX;
			this.posY = posY;
			this.size = size;
			img = image;
		}
		
		public Shot shoot() {//da la capacidad disparo
			return new Shot(posX + size / 2 - Shot.size / 2, posY - Shot.size);
		}

		public void update() {//actualizar estado
			if(exploding) explosionStep++;
			destroyed = explosionStep > EXPLOSION_STEPS;
		}
		
		public void draw() {//evento explosion
			if(exploding) {
				gc.drawImage(EXPLOSION_IMG, explosionStep % EXPLOSION_COL * EXPLOSION_W, (explosionStep / EXPLOSION_ROWS) * EXPLOSION_H + 1,
						EXPLOSION_W, EXPLOSION_H,
						posX, posY, size, size);
			}
			else {
				gc.drawImage(img, posX, posY, size, size);
			}
		}
	
		public boolean colide(Rocket other) {//se crea el evento colide
			int d = distance(this.posX + size / 2, this.posY + size /2, other.posX + other.size / 2, other.posY + other.size / 2);
			return d < other.size / 2 + this.size / 2 ;
		}
		
		public void explode() {//si el usuario explota, indica fin del juego
			exploding = true;
			explosionStep = -1;
		}

	}
	
	//se crean las bombas, los cuales son de clase rocket, la misma clase de "player"
	public class Bomb extends Rocket {
		
		int SPEED = (score/5)+2;
		
		public Bomb(int posX, int posY, int size, Image image) {
			super(posX, posY, size, image);
		}
		
		public void update() {
			super.update();
			if(!exploding && !destroyed) posY += SPEED;
			if(posY > HEIGHT) destroyed = true;
		}
	}

	//aqui se crea shot, la clase encargada de los disparos
	public class Shot {
		
		public boolean toRemove;

		int posX, posY, speed = 10;
		static final int size = 6;
			
		public Shot(int posX, int posY) {
			this.posX = posX;
			this.posY = posY;
		}

		public void update() {
			posY-=speed;
		}
		

		public void draw() {//aqui se crea el "poder especial" tras una cierta cantidad de enemigos derrotados
			gc.setFill(Color.GREEN);
			if (score >=50 && score<=70 || score>=120) {
				gc.setFill(Color.RED);
				speed = 50;
				gc.fillRect(posX-5, posY-10, size+10, size+30);
			} else {
			gc.fillOval(posX, posY, size, size);
			}
		}
		
		public boolean colide(Rocket Rocket) {
			int distance = distance(this.posX + size / 2, this.posY + size / 2, Rocket.posX + Rocket.size / 2, Rocket.posY + Rocket.size / 2);
			return distance  < Rocket.size / 2 + size / 2;
		} 
		
		
	}
	
	//entorno o mapa del juego
	public class Universe {
		int posX, posY;
		private int h, w, r, g, b;
		private double opacity;
		
		public Universe() {
			posX = RAND.nextInt(WIDTH);
			posY = 0;
			w = RAND.nextInt(5) + 1;
			h =  RAND.nextInt(5) + 1;
			r = RAND.nextInt(100) + 150;
			g = RAND.nextInt(100) + 150;
			b = RAND.nextInt(100) + 150;
			opacity = RAND.nextFloat();
			if(opacity < 0) opacity *=-1;
			if(opacity > 0.5) opacity = 0.5;
		}
		
		public void draw() {
			if(opacity > 0.8) opacity-=0.01;
			if(opacity < 0.1) opacity+=0.01;
			gc.setFill(Color.rgb(r, g, b, opacity));
			gc.fillOval(posX, posY, w, h);
			posY+=20;
		}
	}
	
	
	Bomb newBomb() {//se crean las bombas y se actualiza la cantidad a medida que destruimos bombas o estas ya no estan en pantalla
		return new Bomb(50 + RAND.nextInt(WIDTH - 100), 0, PLAYER_SIZE, BOMBS_IMG[RAND.nextInt(BOMBS_IMG.length)]);
	}
	
	int distance(int x1, int y1, int x2, int y2) {
		return (int) Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
	}
	
	
	public static void main(String[] args) {//esta linea representa el llamado de la aplicacion, en este caso no le pasamos parametros por lo 
		launch();
	}
}