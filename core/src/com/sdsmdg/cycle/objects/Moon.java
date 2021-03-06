package com.sdsmdg.cycle.objects;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.sdsmdg.cycle.gameworld.GameWorld;

public class Moon {

    private float radius;
    private Vector2 position;
    private Sprite sprite;
    private float rotation = 0;
    private float w;

    public Moon(Vector2 position, Sprite sprite) {
        this.position = position;
        this.sprite = sprite;

        w = 12;
    }

    public void update(float delta) {
        rotation -= w * delta;
        //This makes the sun also change its size while it rotates, Gives it a more cartoony look! :P
        radius = GameWorld.screenWidth / 10 + (float)Math.sin(4 * Math.toRadians(rotation)) * GameWorld.screenWidth / 200;
    }

    public void onDraw(SpriteBatch batcher) {

        batcher.draw(sprite,
                position.x - radius, position.y - radius,
                radius, radius,
                radius * 2, radius * 2,
                1, 1,
                rotation
        );

    }
}
