package com.bordeen.ld28;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Ellipse;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

public class Character implements InputProcessor {
	Body body;
	TextureRegion[] sheet;
	GameScene gs;
	void create(GameScene gs, TiledMap map, World world, Texture sheet)
	{
		this.gs = gs;
		this.sheet = new TextureRegion[]
				{
				new TextureRegion(sheet, 0, 0, 32, 32),
				new TextureRegion(sheet, 32, 0, 32, 32)
				};
		BodyDef bd = new BodyDef();
		bd.type = BodyType.DynamicBody;
		bd.fixedRotation = true;
		Ellipse charFlag = ((EllipseMapObject)map.getLayers().get(1).getObjects().get(0)).getEllipse();
		bd.position.x = charFlag.x * GameScene.unitScale;
		bd.position.y = charFlag.y * GameScene.unitScale;
		
		body = world.createBody(bd);
		body.setUserData(this);
		
		PolygonShape ps = new PolygonShape();
		ps.setAsBox(0.315f, 0.48f * 0.80f, new Vector2(0, 0.48f * 0.2f), 0);
		Fixture f = body.createFixture(ps, 2);
		f.setUserData(new Integer(0));
		Filter fd = f.getFilterData();
		fd.categoryBits = Filters.character;
		f.setFilterData(fd);
		f.setFriction(0);
		
		ps.setAsBox(0.3149f, 0.48f * 0.20f, new Vector2(0, -0.48f * 0.8f), 0);
		f = body.createFixture(ps, 2);
		f.setUserData(new Integer(1));
		fd = f.getFilterData();
		fd.categoryBits = Filters.character;
		f.setFilterData(fd);
		f.setFriction(0);
		
		ps.setAsBox(0.2f, 0.1f, new Vector2(0f, -0.49f), 0);
		f = body.createFixture(ps, 1);
		f.setSensor(true);
		fd = f.getFilterData();
		fd.categoryBits = Filters.character;
		fd.maskBits &= ~Filters.clock & ~Filters.enemy & ~Filters.character;
		f.setFilterData(fd);
		ps.dispose();
	}
	int footTouching = 0;
	Vector2 pos;
	Vector2 lnVel;
	Vector2 worldCenter;
	void calcVars()
	{
		pos = body.getPosition();
		lnVel = body.getLinearVelocity();
		worldCenter = body.getWorldCenter();
	}
	int currentFrame = 0;
	float frameTimer = 0;
	void draw(SpriteBatch batch)
	{
		sheet[currentFrame].flip(flipX, died);
		batch.draw(sheet[currentFrame], pos.x - 0.5f, pos.y - 0.5f, 1, 1);
		sheet[currentFrame].flip(flipX, died);
	}
	
	int keyState = 0;
	final static int KLEFT = 0x1;
	final static int KRIGHT = 0x2;
	final static int KUP = 0x4;
	final static int KDOWN = 0x8;
	float diedTime = 0;
	public final static float dieInterval = 2.0f;
	void update(float dt)
	{
		if(worldCenter.y < -1)
		{
			die();
		}
		if(died)
		{
			diedTime += dt;
			return;
		}
		float charDesiredVel = 0;
		switch(keyState & (KLEFT | KRIGHT))
		{
		case KLEFT:
			charDesiredVel = -5; break;
		case KRIGHT:
			charDesiredVel = 5; break;
		}
		float velChange = charDesiredVel - lnVel.x;
		float imp = body.getMass() * velChange;
		body.applyLinearImpulse(imp, 0, worldCenter.x, worldCenter.y, true);
		if(jumpTimeout > 0)
		{
			jumpTimeout -= dt;
			if((keyState & KUP) == KUP)
			{
				body.applyLinearImpulse(0, 0.125f * body.getMass() * jumpTimeout, worldCenter.x, worldCenter.y, true);
			}
		}
		if(footTouching < 1)
		{
			currentFrame = 1;
			frameTimer = 0;
		}
		else if(charDesiredVel == 0)
		{
			currentFrame = 0;
			frameTimer = 0;
		}
		else
		{
			frameTimer += dt;
			if(frameTimer >= GameScene.spb * 0.25f)
			{
				frameTimer -= GameScene.spb * 0.25f;
				currentFrame = (currentFrame + 1) % 2;
			}
		}
	}
	boolean flipX = false;
	float jumpTimeout = 0;
	public boolean died = false;
	@Override
	public boolean keyDown(int keycode) {
		if(died) return false;
		switch(keycode)
		{
		case Keys.LEFT:
			Press(KLEFT); break;

		case Keys.RIGHT:
			Press(KRIGHT);  flipX = false; break;

		case Keys.UP:
		case Keys.Z:
		case Keys.X:
		case Keys.C:
		case Keys.A:
		case Keys.Q:
		case Keys.SPACE:
			Press(KUP);
			break;
		case Keys.DOWN:
			Press(KDOWN);
			keyState |= KDOWN; break;
		default:
			return false;
		}
		return true;
	}
	
	void Press(int id)
	{
		switch(id)
		{
		case KLEFT:
			 flipX = true;
			 break;
		case KRIGHT:
			 flipX = false;
			 break;
		case KUP:
			if(footTouching > 0f && jumpTimeout <= 0.8f)
			{
				body.applyLinearImpulse(0, 3.5f * body.getMass(), worldCenter.x, worldCenter.y, true);
				jumpTimeout = 1.0f;
				gs.jump.play();
			}
			 break;
		}
		keyState |= id;
	}
	void Release(int id)
	{
		keyState &= ~id;
	}

	@Override
	public boolean keyUp(int keycode) {
		if(died) return false;
		switch(keycode)
		{
		case Keys.LEFT:
			Release(KLEFT); break;

		case Keys.RIGHT:
			Release(KRIGHT); break;
			
		case Keys.UP:
		case Keys.Z:
		case Keys.X:
		case Keys.C:
		case Keys.A:
		case Keys.Q:
		case Keys.SPACE:
			Release(KUP); break;

		case Keys.DOWN:
			Release(KDOWN); break;
		default:
			return false;
		}
		return true;
	}

	@Override
	public boolean keyTyped(char character) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		// TODO Auto-generated method stub
		return false;
	}

	public void die() {
		if(died) return;
		died = true;
		gs.die.play();
		gs.camera.shake(0.5f, 0.4f, 60);
		diedTime = 0;
		Array<Fixture> fixList = body.getFixtureList();
		for(int i = 0; i < fixList.size; ++i)
		{
			Fixture f = fixList.get(i);
			Filter fd = f.getFilterData();
			fd.maskBits = Filters.scenary;
			f.setFilterData(fd);
			f.setFriction(0.3f);
			f.setRestitution(0.5f);
		}
	}
}
