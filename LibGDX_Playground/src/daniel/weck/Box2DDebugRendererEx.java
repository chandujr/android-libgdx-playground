package daniel.weck;

import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.JointDef.JointType;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape.Type;
import com.badlogic.gdx.physics.box2d.Transform;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.WorldManifold;
import com.badlogic.gdx.physics.box2d.joints.PulleyJoint;

import daniel.weck.LibGDXPlayground.BodyUserData;
import daniel.weck.ShapeRendererEx.ShapeType;

public class Box2DDebugRendererEx {

	/** the immediate mode renderer to output our debug drawings **/
	protected ShapeRendererEx renderer;

	/** a spritebatch and a font for text rendering **/
	public SpriteBatch batch;

	/** vertices for polygon rendering **/
	private static Vector2[] vertices = new Vector2[1000];

	private static Vector2 mLower;
	private static Vector2 mUpper;

	private boolean mDrawBodies;
	private boolean mDrawJoints;
	private boolean mDrawAABBs;

	public Box2DDebugRendererEx() {

		this(true, true, false);
	}

	public Box2DDebugRendererEx(boolean drawBodies, boolean drawJoints,
			boolean drawAABBs) {
		// next we setup the immediate mode renderer
		renderer = new ShapeRendererEx(1000, false);

		// next we create a SpriteBatch and a font
		batch = new SpriteBatch();

		mLower = new Vector2();
		mUpper = new Vector2();

		// initialize vertices array
		for (int i = 0; i < vertices.length; i++)
			vertices[i] = new Vector2();

		mDrawBodies = drawBodies;
		mDrawJoints = drawJoints;
		mDrawAABBs = drawAABBs;
	}

	/** This assumes that the projection matrix has already been set. */
	public void render(World world, Matrix4 projMatrix, boolean drawContacts) {
		renderer.setProjectionMatrix(projMatrix);
		renderBodies(world, drawContacts);
	}

	private final Color COLOR_NOT_ACTIVE = new Color(0.5f, 0.5f, 0.3f, 1);
	private final Color COLOR_ACTIVE_STATIC = new Color(0.5f, 0.9f, 0.5f, 1);
	private final Color COLOR_ACTIVE_KINEMATIC = new Color(0.5f, 0.5f, 0.9f, 1);
	private final Color COLOR_ACTIVE_DYNAMIC_SLEEPING = new Color(0.6f, 0.6f,
			0.6f, 1);
	private final Color COLOR_ACTIVE_DYNAMIC_AWAKE = new Color(0.9f, 0.7f,
			0.7f, 1);
	private final Color JOINT_COLOR = new Color(0.5f, 0.8f, 0.8f, 1);
	private final Color AABB_COLOR = new Color(1.0f, 0, 1.0f, 1f);

	private void renderBodies(World world, boolean drawContacts) {

		Gdx.gl.glLineWidth(3);

		renderer.begin(ShapeType.Line);

		if (mDrawBodies || mDrawAABBs) {
			for (Iterator<Body> iter = world.getBodies(); iter.hasNext();) {
				Body body = iter.next();

				BodyUserData userData = (BodyUserData) body.getUserData();
				if (userData == null)
					continue;

				// System.out.println(userData.zCoord + "---" +
				// userData.textureIndex);

				Transform transform = body.getTransform();
				int len = body.getFixtureList().size();
				List<Fixture> fixtures = body.getFixtureList();
				for (int i = 0; i < len; i++) {
					Fixture fixture = fixtures.get(i);

					if (mDrawBodies) {
						if (body.isActive() == false)
							drawShape(fixture, transform, COLOR_NOT_ACTIVE,
									userData.zCoord);
						else if (body.getType() == BodyType.StaticBody)
							drawShape(fixture, transform, COLOR_ACTIVE_STATIC,
									userData.zCoord);
						else if (body.getType() == BodyType.KinematicBody)
							drawShape(fixture, transform,
									COLOR_ACTIVE_KINEMATIC, userData.zCoord);
						else if (body.isAwake() == false)
							drawShape(fixture, transform,
									COLOR_ACTIVE_DYNAMIC_SLEEPING,
									userData.zCoord);
						else
							drawShape(fixture, transform,
									COLOR_ACTIVE_DYNAMIC_AWAKE, userData.zCoord);
					}

					if (mDrawAABBs) {
						drawAABB(fixture, transform, userData.zCoord);
					}
				}
			}
		}

		if (mDrawJoints) {
			for (Iterator<Joint> iter = world.getJoints(); iter.hasNext();) {
				Joint joint = iter.next();
				BodyUserData userData = (BodyUserData) joint.getBodyB()
						.getUserData();
				if (userData == null) {
					userData = (BodyUserData) joint.getBodyA().getUserData();
				}

				if (userData != null)
					drawJoint(joint, userData.zCoord);
			}
		}

		renderer.end();

		if (!drawContacts)
			return;

		int fps = Gdx.graphics.getFramesPerSecond();
		if (fps < 20)
			return;

		if (Gdx.gl10 != null)
			Gdx.gl10.glPointSize(6);
		renderer.begin(ShapeType.Point);

		List<Contact> contacts = world.getContactList();
		// for (Contact contact : contacts) {
		for (int i = 0; i < contacts.size(); i++) {
			Contact contact = contacts.get(i);
			BodyUserData userData = (BodyUserData) contact.getFixtureB()
					.getBody().getUserData();

			if (userData == null) {
				userData = (BodyUserData) contact.getFixtureA().getBody()
						.getUserData();
			}

			if (userData != null)
				drawContact(contact, userData.zCoord);
		}

		renderer.end();
		if (Gdx.gl10 != null)
			Gdx.gl10.glPointSize(1);
	}

	private void drawAABB(Fixture fixture, Transform transform, float zIndex) {
		if (fixture.getType() == Type.Circle) {

			CircleShape shape = (CircleShape) fixture.getShape();
			float radius = shape.getRadius();
			vertices[0].set(shape.getPosition());
			vertices[0].rotate(transform.getRotation()).add(
					transform.getPosition());
			mLower.set(vertices[0].x - radius, vertices[0].y - radius);
			mUpper.set(vertices[0].x + radius, vertices[0].y + radius);

			// define vertices in ccw fashion...
			vertices[0].set(mLower.x, mLower.y);
			vertices[1].set(mUpper.x, mLower.y);
			vertices[2].set(mUpper.x, mUpper.y);
			vertices[3].set(mLower.x, mUpper.y);

			drawSolidPolygon(vertices, 4, AABB_COLOR, zIndex);
		} else if (fixture.getType() == Type.Polygon) {
			PolygonShape shape = (PolygonShape) fixture.getShape();
			int vertexCount = shape.getVertexCount();

			shape.getVertex(0, vertices[0]);
			mLower.set(transform.mul(vertices[0]));
			mUpper.set(mLower);
			for (int i = 1; i < vertexCount; i++) {
				shape.getVertex(i, vertices[i]);
				transform.mul(vertices[i]);
				mLower.x = Math.min(mLower.x, vertices[i].x);
				mLower.y = Math.min(mLower.y, vertices[i].y);
				mUpper.x = Math.max(mUpper.x, vertices[i].x);
				mUpper.y = Math.max(mUpper.y, vertices[i].y);
			}

			// define vertices in ccw fashion...
			vertices[0].set(mLower.x, mLower.y);
			vertices[1].set(mUpper.x, mLower.y);
			vertices[2].set(mUpper.x, mUpper.y);
			vertices[3].set(mLower.x, mUpper.y);

			drawSolidPolygon(vertices, 4, AABB_COLOR, zIndex);
		}
	}

	private static Vector2 t = new Vector2();
	private static Vector2 axis = new Vector2();

	private void drawShape(Fixture fixture, Transform transform, Color color,
			float zIndex) {
		if (fixture.getType() == Type.Circle) {
			CircleShape circle = (CircleShape) fixture.getShape();
			t.set(circle.getPosition());
			transform.mul(t);
			drawSolidCircle(t, circle.getRadius(), axis.set(
					transform.vals[Transform.COS],
					transform.vals[Transform.SIN]), color, zIndex);
		}

		if (fixture.getType() == Type.Edge) {
			EdgeShape edge = (EdgeShape) fixture.getShape();
			edge.getVertex1(vertices[0]);
			edge.getVertex2(vertices[1]);
			transform.mul(vertices[0]);
			transform.mul(vertices[1]);
			drawSolidPolygon(vertices, 2, color, zIndex);
		}

		if (fixture.getType() == Type.Polygon) {
			PolygonShape chain = (PolygonShape) fixture.getShape();
			int vertexCount = chain.getVertexCount();
			for (int i = 0; i < vertexCount; i++) {
				chain.getVertex(i, vertices[i]);
				transform.mul(vertices[i]);
			}
			drawSolidPolygon(vertices, vertexCount, color, zIndex);
		}

		if (fixture.getType() == Type.Chain) {
			ChainShape chain = (ChainShape) fixture.getShape();
			int vertexCount = chain.getVertexCount();
			for (int i = 0; i < vertexCount; i++) {
				chain.getVertex(i, vertices[i]);
				transform.mul(vertices[i]);
			}
			drawSolidPolygon(vertices, vertexCount, color, zIndex);
		}
	}

	private final Vector2 f = new Vector2();
	private final Vector2 v = new Vector2();
	private final Vector2 lv = new Vector2();

	private void drawSolidCircle(Vector2 center, float radius, Vector2 axis,
			Color color, float zIndex) {
		float angle = 0;
		float angleInc = 2 * (float) Math.PI / 20;
		renderer.setColor(color.r, color.g, color.b, color.a);
		for (int i = 0; i < 20; i++, angle += angleInc) {
			v.set((float) Math.cos(angle) * radius + center.x,
					(float) Math.sin(angle) * radius + center.y);
			if (i == 0) {
				lv.set(v);
				f.set(v);
				continue;
			}
			renderer.line(lv.x, lv.y, zIndex, v.x, v.y, zIndex);
			lv.set(v);
		}
		renderer.line(f.x, f.y, zIndex, lv.x, lv.y, zIndex);
		renderer.line(center.x, center.y, zIndex, center.x + axis.x * radius,
				center.y + axis.y * radius, zIndex);
	}

	private void drawSolidPolygon(Vector2[] vertices, int vertexCount,
			Color color, float zIndex) {
		renderer.setColor(color.r, color.g, color.b, color.a);
		for (int i = 0; i < vertexCount; i++) {
			Vector2 v = vertices[i];
			if (i == 0) {
				lv.set(v);
				f.set(v);
				continue;
			}
			renderer.line(lv.x, lv.y, zIndex, v.x, v.y, zIndex);
			lv.set(v);
		}
		renderer.line(f.x, f.y, zIndex, lv.x, lv.y, zIndex);
	}

	private void drawJoint(Joint joint, float zIndex) {
		Body bodyA = joint.getBodyA();
		Body bodyB = joint.getBodyB();
		Transform xf1 = bodyA.getTransform();
		Transform xf2 = bodyB.getTransform();

		Vector2 x1 = xf1.getPosition();
		Vector2 x2 = xf2.getPosition();
		Vector2 p1 = joint.getAnchorA();
		Vector2 p2 = joint.getAnchorB();

		if (joint.getType() == JointType.DistanceJoint) {
			drawSegment(p1, p2, JOINT_COLOR, zIndex);
		} else if (joint.getType() == JointType.PulleyJoint) {
			PulleyJoint pulley = (PulleyJoint) joint;
			Vector2 s1 = pulley.getGroundAnchorA();
			Vector2 s2 = pulley.getGroundAnchorB();
			drawSegment(s1, p1, JOINT_COLOR, zIndex);
			drawSegment(s2, p2, JOINT_COLOR, zIndex);
			drawSegment(s1, s2, JOINT_COLOR, zIndex);
		} else if (joint.getType() == JointType.MouseJoint) {
			drawSegment(joint.getAnchorA(), joint.getAnchorB(), JOINT_COLOR,
					zIndex);
		} else {
			drawSegment(x1, p1, JOINT_COLOR, zIndex);
			drawSegment(p1, p2, JOINT_COLOR, zIndex);
			drawSegment(x2, p2, JOINT_COLOR, zIndex);
		}
	}

	private void drawSegment(Vector2 x1, Vector2 x2, Color color, float zIndex) {
		renderer.setColor(color);
		renderer.line(x1.x, x1.y, zIndex, x2.x, x2.y, zIndex);
	}

	private void drawContact(Contact contact, float zIndex) {
		WorldManifold worldManifold = contact.getWorldManifold();
		if (worldManifold.getNumberOfContactPoints() == 0)
			return;
		Vector2 point = worldManifold.getPoints()[0];
		renderer.point(point.x, point.y, zIndex);
	}

	public void dispose() {
		batch.dispose();
	}
}
