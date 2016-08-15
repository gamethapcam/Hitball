package com.sdsmdg.cycle.gameworld;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.Vector2;
import com.sdsmdg.cycle.chelpers.AssetLoader;
import com.sdsmdg.cycle.objects.Ball;
import com.sdsmdg.cycle.objects.Bat;
import com.sdsmdg.cycle.objects.Button;
import com.sdsmdg.cycle.objects.Cloud;
import com.sdsmdg.cycle.objects.Fan;
import com.sdsmdg.cycle.objects.Sun;

import java.util.ArrayList;
import java.util.List;

public class GameWorld {

    //This TAG will be used while logging in the app(for debugging purposes)
    //The TAG will be the class name, BTW
    private final String TAG = GameWorld.class.getSimpleName();

    private List<Ball> balls = new ArrayList<Ball>();
    private Bat bat;
    private int screenWidth, screenHeight;
    GameState gameState;
    Button replayButton;
    public static final int PLAY = 0;
    public static final int REPLAY = 1;
    public static int score = 0;
    Preferences prefs;
    private List<Cloud> clouds = new ArrayList<Cloud>();
    private Fan fan;
    private Sun sun;

    private enum GameState {
        READY, RUNNING, OVER
    }

    public boolean isReady() {
        return gameState == GameState.READY;
    }

    public boolean isRunning() {
        return gameState == GameState.RUNNING;
    }

    public boolean isOver() {
        return gameState == GameState.OVER;
    }

    public GameWorld(int screenWidth, int screenHeight) {
        Vector2 batPosition = new Vector2(screenWidth / 10, screenHeight / 2 + 100);
        int batHeight = screenWidth / 15;
        int batWidth = (AssetLoader.batRegion.getRegionWidth() * batHeight) / AssetLoader.batRegion.getRegionHeight();
        bat = new Bat(batWidth, batHeight, batPosition);
        Ball ball1 = new Ball(screenWidth, screenHeight);
        balls.add(ball1);
        this.screenHeight = screenHeight;
        this.screenWidth = screenWidth;
        gameState = GameState.READY;

        int replayWidth = screenWidth / 4;
        int replayHeight = screenWidth / 4;
        replayButton = new Button(this, (screenWidth - replayWidth) / 2, 0.75f * screenHeight - replayHeight / 2,
                replayWidth, replayHeight,
                AssetLoader.replayRegionOn, AssetLoader.replayRegionOff);

        float cloudWidth = screenWidth / 3;
        float cloudHeight = (cloudWidth * 121) / 232;
        clouds.add(new Cloud(this, cloudWidth, cloudHeight,
                new Vector2(screenWidth / 10, screenHeight / 20),//cloud's positions
                new Vector2(screenWidth / 2000f, 0),//cloud's velocity
                AssetLoader.cloudRegion));

        cloudWidth /= 2;
        cloudHeight /= 2;
        clouds.add(new Cloud(this, cloudWidth, cloudHeight,
                new Vector2(screenWidth - cloudWidth, cloudHeight / 2),
                new Vector2(screenWidth / 1500f, 0),
                AssetLoader.cloud1Region));

        fan = new Fan(this,
                screenWidth / 10, screenWidth / 10,
                new Vector2(screenWidth / 7.5f, screenHeight / 3),
                AssetLoader.fanRegion);

        sun = new Sun(this,
                new Vector2(screenWidth * 0.75f, screenHeight / 3),
                AssetLoader.sunRegion);

        Gdx.app.log(TAG, "screenWidth : " + screenWidth + " screenHeight : " + screenHeight);
        prefs = Gdx.app.getPreferences("Highscore");
    }

    public void setHighScore(int score) {
        prefs.putInteger("highscore", score);
        prefs.flush();
    }

    public void update(float delta) {

        /*
        These are the objects that need to be updated in every state of the game
         */
        sun.update(delta);

        for (int i = 0; i < clouds.size(); i++) {
            clouds.get(i).update(delta);
        }
        bat.update(delta);
        fan.update(delta);

        /*
        While some other objects need to be updated at a certain state of the game
         */
        switch (gameState) {
            case READY:
                updateReady(delta);
                break;
            case OVER:
                updateOver(delta);
                break;
            default:
                updateRunning(delta);
        }
    }

    public int getHighScore() {
        return prefs.getInteger("highscore");
    }

    public void updateReady(float delta) {

    }

    public void updateOver(float delta) {

    }

    public void updateRunning(float delta) {
        //Update all balls positions on screen
        for (int i = 0; i < balls.size(); i++) {
            balls.get(i).update(delta);
        }

        for (int i = 0; i < balls.size(); i++) {
            Ball ball = balls.get(i);
            if (isColliding(bat, ball, delta) && ball.isInPlane()) {
                playHitSound();
                setBallOut(ball);
                int distance = getDistance(ball.getPosition().x, bat.getOriginX(), ball.getPosition().y, bat.getOriginY());
                int vBat = 0;
                if (bat.isRotating() && bat.getW() < 0) {
                    vBat = (int) Math.toRadians(bat.getW()) * distance;
                }
                ball.afterCollisionWithBody((int) bat.getRotation(), vBat);
                updateScore();
            } else if (isCollidingHandle(bat, ball, delta) && ball.isInPlane()) {
                playHitSound();
                setBallOut(ball);
                ball.setOffPlane();
            }
            //To avoid overlap of bat and ball
            if (ball.isBallOffScreen()) {
                ball.setPosition(screenWidth / 2, screenHeight / 3);
                ball.setRadius(screenWidth / 20);
                setGameStateOver();
            }
        }

    }

    public void playHitSound() {
        AssetLoader.sound.play();//Play the hit sound when the ball hits the bat body and the handle
    }

    public void updateScore() {
        score++;
        if (score > getHighScore()) {
            setHighScore(score);
        }
    }

    public void setBallOut(Ball ball) {
        ball.getPosition().y = (int) ball.getRotatedY(-bat.getRotation(), bat.getOriginX(), bat.getOriginY(), (int) ball.getPosition().x, (int) bat.getPosition().y - ball.getRadius());
    }

    public int getDistance(double x1, double x2, double y1, double y2) {
        return (int) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    /*
    This function checks whether the bat body(bat without the handle) collides with ball or not)
    For checking collision between bat handle and ball there is isCollidingHandle()
     */
    public boolean isColliding(Bat bat, Ball ball, float delta) {

        double rectx = bat.getCenterBodyX();
        double recty = bat.getCenterBodyY();

        double circleDistX = Math.abs(ball.getRotatedX(bat.getRotation(), bat.getOriginX(), bat.getOriginY(), (int) ball.getNextX(delta), (int) ball.getNextY(delta)) - rectx);
        double circleDistY = Math.abs(ball.getRotatedY(bat.getRotation(), bat.getOriginX(), bat.getOriginY(), (int) ball.getNextX(delta), (int) ball.getNextY(delta)) - recty);

        //when ball bottom just touches the bat body(bat without handle)
        if (circleDistX > bat.getWidthWithoutHandle() / 2 + ball.getRadius()) {
            return false;
        }
        if (circleDistY > (bat.getHeight() / 2 + ball.getRadius())) {
            return false;
        }

        if (circleDistX <= (bat.getWidthWithoutHandle() / 2 - ball.getRadius())) {
            return true;
        }
        if (circleDistY <= (bat.getHeight() / 2)) {
            return true;
        }

        double cornerDistanceSq = Math.pow((circleDistX - (bat.getWidthWithoutHandle() / 2 - ball.getRadius()) / 2), 2) +
                Math.pow((circleDistY - bat.getHeight() / 2), 2);

        return (cornerDistanceSq < Math.pow(ball.getRadius(), 2));
    }

    public boolean isCollidingHandle(Bat bat, Ball ball, float delta) {

        double rectx = bat.getCenterHandleX();
        double recty = bat.getCenterHandleY();

        double circleDistX = Math.abs(ball.getRotatedX(bat.getRotation(), bat.getOriginX(), bat.getOriginY(), (int) ball.getNextX(delta), (int) ball.getNextY(delta)) - rectx);
        double circleDistY = Math.abs(ball.getRotatedY(bat.getRotation(), bat.getOriginX(), bat.getOriginY(), (int) ball.getNextX(delta), (int) ball.getNextY(delta)) - recty);

        //when ball bottom just touches the bat body(bat without handle)
        if (circleDistX > bat.getHandleWidth() / 2 + ball.getRadius()) {
            return false;
        }
        if (circleDistY > (bat.getHeight() / 2 + ball.getRadius())) {
            return false;
        }

        if (circleDistX <= (bat.getHandleWidth() / 2 - ball.getRadius())) {
            return true;
        }
        if (circleDistY <= (bat.getHeight() / 2)) {
            return true;
        }

        double cornerDistanceSq = Math.pow((circleDistX - (bat.getHandleWidth() / 2 - ball.getRadius()) / 2), 2) +
                Math.pow((circleDistY - bat.getHeight() / 2), 2);

        return (cornerDistanceSq < Math.pow(ball.getRadius(), 2));
    }

    public Ball getBall(int i) {
        return balls.get(i);
    }

    public Bat getBat() {
        return bat;
    }

    public Button getReplayButton() {
        return replayButton;
    }

    public void setGameStateRunning() {
        for (int i = 0; i < balls.size(); i++) {
            getBall(i).reset();
        }
        score = 0;
        gameState = GameState.RUNNING;
    }

    public void setGameStateOver() {
        bat.onTouchUp();
        gameState = GameState.OVER;
    }

    public int getScore() {
        return score;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public List<Cloud> getClouds() {
        return clouds;
    }

    public Fan getFan() {
        return fan;
    }

    public Sun getSun() {
        return sun;
    }
}
