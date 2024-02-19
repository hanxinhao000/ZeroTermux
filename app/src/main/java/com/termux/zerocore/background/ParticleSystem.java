package com.termux.zerocore.background;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.PropertyListParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


/**
 * Created by qinfeng on 16/8/4.
 */

public abstract class ParticleSystem {
    
    /** Creates an initializes a ParticleSystem from a plist file.
     This plist files can be created manually or with Particle Designer:
     http://particledesigner.71squared.com/
     */
     ParticleSystem(Context context, int plistFile, int resourceId) {
         this.context = context;
         initWithFile(plistFile,resourceId);

     }

    public void update(float dt) {
        if (_isActive &&  _emissionRate != 0f)
        {
            float rate = 1.0f / 30;
            //issue #1201, prevent bursts of particles, due to too high emitCounter
            if (_particleCount < _totalParticles)
            {
                _emitCounter += dt;
            }

            while (_particleCount < _totalParticles && _emitCounter > rate )
            {
                addParticle();
                _emitCounter -= rate;
            }



            _elapsed += dt;
            if (_duration != -1 && _duration < _elapsed)
            {
//                stopSystem();
            }
        }

        _particleIdx = 0;

        Vec2 currentPosition = new Vec2(0f,0f);
        if (_positionType == PositionType.FREE)
        {
            currentPosition = new Vec2(0f,0f);
        }
        else if (_positionType == PositionType.RELATIVE)
        {
            currentPosition = _position;
        }


        {

            clearData();

            Vec2 newPos = new Vec2(0f,0f);
            Vec2 radial= new Vec2(0f,0f);
            Vec2 tangential= new Vec2(0f,0f);

            while (_particleIdx < _particleCount)
            {
                Particle p = _particles.get(_particleIdx);

                // life
                p.timeToLive -= dt;



                if (p.timeToLive > 0) {
                    // Mode A: gravity, direction, tangential accel & radial accel
                    if (_emitterMode == Mode.GRAVITY) {

                        radial.x = 0f;
                        radial.y = 0f;
                        tangential.x = 0f;
                        tangential.y = 0f;

                        // radial acceleration
                        if (p.pos.x != 0|| p.pos.y != 0) {
                            radial.x  =  p.pos.x / (float) Math.sqrt(p.pos.x * p.pos.x + p.pos.y * p.pos.y);
                            radial.y  =  p.pos.y / (float) Math.sqrt(p.pos.x * p.pos.x + p.pos.y * p.pos.y);
                        }
                        tangential = radial;
                        radial.x = radial.x * p.modeA.radialAccel;
                        radial.y = radial.y * p.modeA.radialAccel;

                        // tangential acceleration
                        float newy = tangential.x;
                        tangential.x = -tangential.y;
                        tangential.y = newy;

                        tangential.x = tangential.x * p.modeA.tangentialAccel;
                        tangential.y = tangential.y * p.modeA.tangentialAccel;
                        // (gravity + radial + tangential) * dt
                        p.modeA.dir.x = p.modeA.dir.x + ((radial.x + tangential.x + gravityX) * dt);
                        p.modeA.dir.y = p.modeA.dir.y + ((radial.y + tangential.y + gravityY) * dt);


                        p.pos.x = p.pos.x + (p.modeA.dir.x * dt * _yCoordFlipped);
                        p.pos.y = p.pos.y + (p.modeA.dir.y * dt * _yCoordFlipped);

                    } else {
                        // Mode B: radius movement
                        // Update the angle and radius of the particle.
                        p.modeB.angle += p.modeB.degreesPerSecond * dt;
                        p.modeB.radius += p.modeB.deltaRadius * dt;

                        p.pos.x = -(float) Math.cos(p.modeB.angle) * p.modeB.radius;
                        p.pos.y = - (float) Math.sin(p.modeB.angle) * p.modeB.radius;
                        p.pos.y *= _yCoordFlipped;
                    }

                    // color
                    p.color.r += (p.deltaColor.r * dt);
                    p.color.g += (p.deltaColor.g * dt);
                    p.color.b += (p.deltaColor.b * dt);
                    p.color.a += (p.deltaColor.a * dt);

                    // diameter
                    p.size += (p.deltaSize * dt);
                    p.size = Math.max( 0, p.size );

                    // angle
                    p.rotation += (p.deltaRotation * dt);

                    //
                    // update values in quad
                    //



                    if (_positionType == PositionType.FREE) { // p.startPos == VEC2_ZERO
                        newPos.x = p.pos.x - (currentPosition.x - p.startPos.x) + _position.x;
                        newPos.y = p.pos.y - (currentPosition.y - p.startPos.y) + _position.y;
                    } else if(_positionType == PositionType.RELATIVE) {

                        newPos.x = p.pos.x - (currentPosition.x - p.startPos.x);
                        newPos.y = p.pos.y - (currentPosition.y - p.startPos.y);
                    } else {
                        newPos = p.pos;
                    }

                    // translate newPos to correct _position, since matrix transform isn't performed in batchnode
                    // don't update the particle with the new _position information, it will interfere with the radius and tangential calculations
                    if (_batchNode) {
//                        newPos.x+=_position.x;
//                        newPos.y+=_position.y;
                    }

                    updateQuadWithParticle(p, newPos);
                    //updateParticleImp(self, updateParticleSel, p, newPos);

                    // update particle counter
                    ++_particleIdx;
                } else {
                    // life < 0
                    int currentIndex = p.atlasIndex;
                    if( _particleIdx != _particleCount-1 ) {

                        Collections.rotate(_particles.subList(_particleIdx, _particleCount), -1);
//                        particle = _particles.get(_particleCount-1);
                    }
                    if (_batchNode) {
//                        //disable the switched particle
//                        _batchNode.disableParticle(_atlasIndex+currentIndex);
//
//                        //switch indexes
//                        _particles[_particleCount-1].atlasIndex = currentIndex;
                    }


                    --_particleCount;

                    if( _particleCount == 0 && _isAutoRemoveOnFinish ) {
                        _unschedule = true;
                        return;
                    }
                }
            } //while
            _transformSystemDirty = false;
        }

        // only update gl buffer when visible
        if (_visible) {

            postStep();
        }

    }
    abstract void setTexture(int resourceId);
    abstract void setTexture(Bitmap texture);
    public abstract void updateBuffer();

    public abstract void draw();

    abstract void updateQuadWithParticle(Particle particle, Vec2 newPosition);
    abstract void clearData();


    private void initWithFile(int plistFile,int resourceId) {
        final InputStream inputStream = context.getResources().openRawResource(
                plistFile);

        try {
            NSDictionary rootDict = (NSDictionary) PropertyListParser.parse(inputStream);
            do
            {
                int maxParticles = ((NSNumber)rootDict.get("maxParticles")).intValue();

////              texture
////              Try to get the texture from the cache
//                String textureName = ((NSString)rootDict.get("textureFileName")).toString();
//
//                int index = textureName.indexOf(".");
//                if (index > 0)
//                    textureName = textureName.substring(0, index);
//
//                Resources resources = context.getResources();
//                final int resourceId = resources.getIdentifier(textureName, "drawable",
//                        context.getPackageName());

                setTexture(resourceId);

                // self, not super
                if(initWithTotalParticles(maxParticles))
                {
                    // Emitter name in particle designer 2.0
//                    _configName =((NSString)rootDict.get("configName")).toString();

                    // angle
                    _angle = ((NSNumber)rootDict.get("angle")).floatValue();
                    _angleVar = ((NSNumber)rootDict.get("angleVariance")).floatValue();

                    // duration
                    _duration =((NSNumber)rootDict.get("duration")).floatValue();

                    // blend function
//                    if (_configName.length()>0)
//                    {
//                        _blendFunc.src = ((NSNumber)rootDict.get("blendFuncSource")).intValue();
//                    }
//                    else
//                    {
//                        _blendFunc.src = ((NSNumber)rootDict.get("blendFuncSource")).intValue();
//                    }


                    _blendFunc.src = ((NSNumber)rootDict.get("blendFuncSource")).intValue();
                    _blendFunc.dst = ((NSNumber)rootDict.get("blendFuncDestination")).intValue();

                    // color
                    _startColor.r = ((NSNumber)rootDict.get("startColorRed")).floatValue();
                    _startColor.g = ((NSNumber)rootDict.get("startColorGreen")).floatValue();
                    _startColor.b = ((NSNumber)rootDict.get("startColorBlue")).floatValue();
                    _startColor.a = ((NSNumber)rootDict.get("startColorAlpha")).floatValue();

                    _startColorVar.r = ((NSNumber)rootDict.get("startColorVarianceRed")).floatValue();
                    _startColorVar.g = ((NSNumber)rootDict.get("startColorVarianceGreen")).floatValue();
                    _startColorVar.b = ((NSNumber)rootDict.get("startColorVarianceBlue")).floatValue();
                    _startColorVar.a = ((NSNumber)rootDict.get("startColorVarianceAlpha")).floatValue();

                    _endColor.r = ((NSNumber)rootDict.get("finishColorRed")).floatValue();
                    _endColor.g = ((NSNumber)rootDict.get("finishColorGreen")).floatValue();
                    _endColor.b = ((NSNumber)rootDict.get("finishColorBlue")).floatValue();
                    _endColor.a = ((NSNumber)rootDict.get("finishColorAlpha")).floatValue();

                    _endColorVar.r = ((NSNumber)rootDict.get("finishColorVarianceRed")).floatValue();
                    _endColorVar.g = ((NSNumber)rootDict.get("finishColorVarianceGreen")).floatValue();
                    _endColorVar.b = ((NSNumber)rootDict.get("finishColorVarianceBlue")).floatValue();
                    _endColorVar.a = ((NSNumber)rootDict.get("finishColorVarianceAlpha")).floatValue();

                    // particle diameter
                    _startSize = ((NSNumber)rootDict.get("startParticleSize")).floatValue();
                    _startSizeVar = ((NSNumber)rootDict.get("startParticleSizeVariance")).floatValue();
                    _endSize = ((NSNumber)rootDict.get("finishParticleSize")).floatValue();
                    _endSizeVar = ((NSNumber)rootDict.get("finishParticleSizeVariance")).floatValue();

                    // _position
                    float x = ((NSNumber)rootDict.get("sourcePositionx")).floatValue();
                    float y = ((NSNumber)rootDict.get("sourcePositiony")).floatValue();
                    _position = new Vec2(x,y);
                    _posVar.x = ((NSNumber)rootDict.get("sourcePositionVariancex")).floatValue();
                    _posVar.y = ((NSNumber)rootDict.get("sourcePositionVariancey")).floatValue();

                    // Spinning
                    _startSpin = ((NSNumber)rootDict.get("rotationStart")).floatValue();
                    _startSpinVar = ((NSNumber)rootDict.get("rotationStartVariance")).floatValue();
                    _endSpin= ((NSNumber)rootDict.get("rotationEnd")).floatValue();
                    _endSpinVar= ((NSNumber)rootDict.get("rotationEndVariance")).floatValue();
                    if (((NSNumber)rootDict.get("emitterType")).intValue() == 0){
                        _emitterMode = Mode.GRAVITY;
                    }else if (((NSNumber)rootDict.get("emitterType")).intValue() == 1) {
                        _emitterMode = Mode.RADIUS;
                    }



                    // Mode A: Gravity + tangential accel + radial accel
                    if (_emitterMode == Mode.GRAVITY)
                    {
                        //TODO modeA.gravity.x always changed
                        // gravity
                        modeA.gravity.x = ((NSNumber)rootDict.get("gravityx")).floatValue();
                        modeA.gravity.y = ((NSNumber)rootDict.get("gravityy")).floatValue();

                        gravityX = ((NSNumber)rootDict.get("gravityx")).floatValue();
                        gravityY = ((NSNumber)rootDict.get("gravityy")).floatValue();
                        // speed
                        modeA.speed = ((NSNumber)rootDict.get("speed")).floatValue();
                        modeA.speedVar = ((NSNumber)rootDict.get("speedVariance")).floatValue();

                        // radial acceleration
                        modeA.radialAccel = ((NSNumber)rootDict.get("radialAcceleration")).floatValue();
                        modeA.radialAccelVar = ((NSNumber)rootDict.get("radialAccelVariance")).floatValue();

                        // tangential acceleration
                        modeA.tangentialAccel = ((NSNumber)rootDict.get("tangentialAcceleration")).floatValue();
                        modeA.tangentialAccelVar = ((NSNumber)rootDict.get("tangentialAccelVariance")).floatValue();

                        // rotation is dir
//                        modeA.rotationIsDir = ((NSNumber)rootDict.get("rotationIsDir")).boolValue();
                    }

                    // or Mode B: radius movement
                    else if (_emitterMode == Mode.RADIUS)
                    {
//                        if (_configName.length()>0)
//                        {
//                            modeB.startRadius = ((NSNumber)rootDict.get("maxRadius")).intValue();
//                        }
//                        else
//                        {
//                            modeB.startRadius = ((NSNumber)rootDict.get("maxRadius")).floatValue();
//                        }
                        modeB.startRadius = ((NSNumber)rootDict.get("maxRadius")).floatValue();
                        modeB.startRadiusVar = ((NSNumber)rootDict.get("maxRadiusVariance")).floatValue();
//                        if (_configName.length()>0)
//                        {
//                            modeB.endRadius = ((NSNumber)rootDict.get("minRadius")).intValue();
//                        }
//                        else
//                        {
//                            modeB.endRadius = ((NSNumber)rootDict.get("minRadius")).floatValue();
//                        }
                        modeB.endRadius = ((NSNumber)rootDict.get("minRadius")).floatValue();

                        if (rootDict.get("minRadiusVariance") != null)
                        {
                            modeB.endRadiusVar = ((NSNumber)rootDict.get("minRadiusVariance")).floatValue();
                        }
                        else
                        {
                            modeB.endRadiusVar = 0.0f;
                        }

//                        if (_configName.length()>0)
//                        {
//                            modeB.rotatePerSecond = ((NSNumber)rootDict.get("rotatePerSecond")).intValue();
//                        }
//                        else
//                        {
//                            modeB.rotatePerSecond = ((NSNumber)rootDict.get("rotatePerSecond")).floatValue();
//                        }
                        modeB.rotatePerSecond = ((NSNumber)rootDict.get("rotatePerSecond")).floatValue();
                        modeB.rotatePerSecondVar = ((NSNumber)rootDict.get("rotatePerSecondVariance")).floatValue();

                    }

                    // life span
                    _life = ((NSNumber)rootDict.get("particleLifespan")).floatValue();
                    _lifeVar = ((NSNumber)rootDict.get("particleLifespanVariance")).floatValue();

                    // emission Rate
                    _emissionRate = _totalParticles / _life ;


                }
            } while (false);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }




    boolean initWithTotalParticles(int totalParticles) {
        _totalParticles = totalParticles;
        _particles = new ArrayList<>(totalParticles);
        for (int i = 0; i < totalParticles; i++) {
            _particles.add(new Particle());
        }
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        _density = metrics.density;
        _width = metrics.widthPixels;
        _height = metrics.heightPixels;

        // default, active
        _isActive = true;

        // default blend function
        _blendFunc = new BlendFunc(Constants.GL_ONE, Constants.GL_ONE_MINUS_SRC_ALPHA);

        // default movement type;
        _positionType = PositionType.FREE;

        // by default be in mode A:
        _emitterMode = Mode.GRAVITY;

        // default: modulate
        // FIXME. not used
        //    colorModulate = YES;

        _isAutoRemoveOnFinish = false;

        // Optimization: compile updateParticle method
        //updateParticleSel = @selector(updateQuadWithParticle:newPosition:);
        //updateParticleImp = (CC_UPDATE_PARTICLE_IMP) [self methodForSelector:updateParticleSel];
        //for batchNode
        _transformSystemDirty = false;

        return true;
    }

    /** Create a system with a fixed number of particles.
     *
     * @param numberOfParticles A given number of particles.
     * @return An autoreleased ParticleSystemQuad object.
     * @js NA
     */
     ParticleSystem(Context context, int numberOfParticles) {
         this.context = context;
         initWithTotalParticles(numberOfParticles);

     }

    /** Add a particle to the emitter.
     *
     * @return True if add success.
     * @js ctor
     */
    boolean addParticle() {
        if (isFull())
        {
            return false;
        }

        Particle particle = _particles.get(_particleCount);
        initParticle(particle);
        ++_particleCount;
        return true;
    }

    /** Initializes a particle.
     *
     * @param particle A given particle pointer.
     */
    void initParticle(Particle particle) {
        // timeToLive
        // no negative life. prevent division by 0
        particle.timeToLive = _life + _lifeVar * CCRANDOM_MINUS1_1();
        particle.timeToLive = Math.abs(particle.timeToLive );
        particle.timeToLive = Math.max(0.1f, particle.timeToLive);

        //TODO fix timeToLive is 0 bug

        // position
        particle.pos.x = _sourcePosition.x + _posVar.x * CCRANDOM_MINUS1_1();

        particle.pos.y = _sourcePosition.y + _posVar.y * CCRANDOM_MINUS1_1();


        // Color
        Color4F start = new Color4F();
        start.r = clampf(_startColor.r + _startColorVar.r * CCRANDOM_MINUS1_1(), 0, 1);
        start.g = clampf(_startColor.g + _startColorVar.g * CCRANDOM_MINUS1_1(), 0, 1);
        start.b = clampf(_startColor.b + _startColorVar.b * CCRANDOM_MINUS1_1(), 0, 1);
        start.a = clampf(_startColor.a + _startColorVar.a * CCRANDOM_MINUS1_1(), 0, 1);

        Color4F end = new Color4F();
        end.r = clampf(_endColor.r + _endColorVar.r * CCRANDOM_MINUS1_1(), 0, 1);
        end.g = clampf(_endColor.g + _endColorVar.g * CCRANDOM_MINUS1_1(), 0, 1);
        end.b = clampf(_endColor.b + _endColorVar.b * CCRANDOM_MINUS1_1(), 0, 1);
        end.a = clampf(_endColor.a + _endColorVar.a * CCRANDOM_MINUS1_1(), 0, 1);

        particle.color = start;
        particle.deltaColor.r = (end.r - start.r) / particle.timeToLive;
        particle.deltaColor.g = (end.g - start.g) / particle.timeToLive;
        particle.deltaColor.b = (end.b - start.b) / particle.timeToLive;
        particle.deltaColor.a = (end.a - start.a) / particle.timeToLive;

        // diameter
        float startS = _startSize + _startSizeVar * CCRANDOM_MINUS1_1();
        startS = Math.max(0, startS); // No negative value

        particle.size = startS;

        if (_endSize == START_SIZE_EQUAL_TO_END_SIZE)
        {
            particle.deltaSize = 0;
        }
        else
        {
            float endS = _endSize + _endSizeVar * CCRANDOM_MINUS1_1();
            endS = Math.max(0, endS); // No negative values
            particle.deltaSize = (endS - startS) / particle.timeToLive;
        }

        // rotation
        float startA = _startSpin + _startSpinVar * CCRANDOM_MINUS1_1();
        float endA = _endSpin + _endSpinVar * CCRANDOM_MINUS1_1();
        particle.rotation = startA;
        particle.deltaRotation = (endA - startA) / particle.timeToLive;

        // position
        if (_positionType == PositionType.FREE)
        {
            particle.startPos = new Vec2(0f,0f);
        }
        else if (_positionType == PositionType.RELATIVE)
        {
            particle.startPos = _position;
        }

        // direction
        float a = CC_DEGREES_TO_RADIANS( _angle + _angleVar * CCRANDOM_MINUS1_1() );

        // Mode Gravity: A
        if (_emitterMode == Mode.GRAVITY)
        {
            Vec2 v = new Vec2((float) Math.cos(a), (float) Math.sin(a));
            float s = modeA.speed + modeA.speedVar * CCRANDOM_MINUS1_1();

            // direction
            particle.modeA.dir = v.scale(s) ;

            // radial accel
            particle.modeA.radialAccel = modeA.radialAccel + modeA.radialAccelVar * CCRANDOM_MINUS1_1();


            // tangential accel
            particle.modeA.tangentialAccel = modeA.tangentialAccel + modeA.tangentialAccelVar * CCRANDOM_MINUS1_1();

            // rotation is dir
            if(modeA.rotationIsDir)
                particle.rotation = -CC_RADIANS_TO_DEGREES(particle.modeA.dir.getAngle());
        }

        // Mode Radius: B
        else
        {
            // Set the default diameter of the particle from the source position
            float startRadius = modeB.startRadius + modeB.startRadiusVar * CCRANDOM_MINUS1_1();
            float endRadius = modeB.endRadius + modeB.endRadiusVar * CCRANDOM_MINUS1_1();

            particle.modeB.radius = startRadius;

            if (modeB.endRadius == START_RADIUS_EQUAL_TO_END_RADIUS)
            {
                particle.modeB.deltaRadius = 0;
            }
            else
            {
                particle.modeB.deltaRadius = (endRadius - startRadius) / particle.timeToLive;
            }

            particle.modeB.angle = a;
            particle.modeB.degreesPerSecond = CC_DEGREES_TO_RADIANS(modeB.rotatePerSecond + modeB.rotatePerSecondVar * CCRANDOM_MINUS1_1());
        }
    }

    public void setTotalParticles(int var) {
        _totalParticles = var;
    }


    /** Stop emitting particles. Running particles will continue to run until they die.
     */
    void stopSystem() {
        _isActive = false;
        _elapsed = _duration;
        _emitCounter = 0;

    }
    /** Kill all living particles.
     */
    void resetSystem() {
        _isActive = true;
        _elapsed = 0;
        for (_particleIdx = 0; _particleIdx < _particleCount; ++_particleIdx)
        {
            Particle particle = _particles.get(_particleIdx);
            particle.timeToLive = 0;
        }
    }
    /** Whether or not the system is full.
     *
     * @return True if the system is full.
     */
    boolean isFull() {
        return _particleCount == _totalParticles;
    }

    /** Update the verts _position data of particle,
     should be overridden by subclasses.
     *
     * @param particle A certain particle.
     * @param newPosition A new _position.
     */
    void updateQuadWithParticle(List<Particle> particle, Vec2 newPosition) {

    }
    /** Update the VBO verts buffer which does not use batch node,
     should be overridden by subclasses. */
    void postStep() {

    }

    /** Call the update mathod with no time..
     */
    void updateWithNoTime() {

    }




    boolean _visible = true;
    Vec2 _position = new Vec2(0f,0f);
    private boolean _batchNode = true;
    private boolean _unschedule = false;


    Context context;

    float _density;

    int _width;
    int _height;
    boolean _isBlendAdditive = false;

    /** whether or not the node will be auto-removed when it has no particles left.
     By default it is false.
     @since v0.8
     */
    boolean _isAutoRemoveOnFinish = false;
    String _plistFile = "";
    //! time elapsed since the start of the system (in seconds)
    float _elapsed ;



    //! Array of particles
    List<Particle> _particles = new ArrayList<>() ;

    //Emitter name
    String _configName = "";

    // color modulate
    //    BOOL colorModulate;

    //! How many particles can be emitted per second
    float _emitCounter;

    //!  particle idx
    int _particleIdx ;


    // index of system in batch node array
    int _atlasIndex;

    //true if scaled or rotated
    boolean _transformSystemDirty = false;
    // Number of allocated particles
    int _allocatedParticles;

    /** Is the emitter active */
    boolean _isActive = true;

    /** Quantity of particles that are being simulated at the moment */
    int _particleCount = 0 ;
    /** How many seconds the emitter will run. -1 means 'forever' */
    float _duration;
    /** sourcePosition of the emitter */
    Vec2 _sourcePosition = new Vec2(0f,0f);
    /** Position variance of the emitter */
    Vec2 _posVar = new Vec2(0f,0f);
    /** life, and life variation of each particle */
    float _life;
    /** life variance of each particle */
    float _lifeVar;
    /** angle and angle variation of each particle */
    float _angle;
    /** angle variance of each particle */
    float _angleVar;

    /** Switch between different kind of emitter modes:
     - kParticleModeGravity: uses gravity, speed, radial and tangential acceleration
     - kParticleModeRadius: uses radius movement + rotation
     */
    Mode _emitterMode = Mode.GRAVITY;

    /** start diameter in pixels of each particle */
    float _startSize;
    /** diameter variance in pixels of each particle */
    float _startSizeVar;
    /** end diameter in pixels of each particle */
    float _endSize;
    /** end diameter variance in pixels of each particle */
    float _endSizeVar;
    /** start color of each particle */
    Color4F _startColor = new Color4F();
    /** start color variance of each particle */
    Color4F _startColorVar = new Color4F();
    /** end color and end color variation of each particle */
    Color4F _endColor = new Color4F();
    /** end color variance of each particle */
    Color4F _endColorVar = new Color4F();
    //* initial angle of each particle
    float _startSpin;
    //* initial angle of each particle
    float _startSpinVar;
    //* initial angle of each particle
    float _endSpin;
    //* initial angle of each particle
    float _endSpinVar;
    /** emission rate of the particles */
    float _emissionRate;



    /** Math.maximum particles of the system */
    int _totalParticles;
    /** conforms to CocosNodeTexture protocol */
    int _textureResourceId;
    /** conforms to CocosNodeTexture protocol */
    BlendFunc _blendFunc;
    /** does the alpha value modify color */
    boolean _opacityModifyRGB = false;
    /** does FlippedY variance of each particle */
    int _yCoordFlipped = 1;

    /** particles movement type: Free or Grouped
     @since v0.8
     */
    PositionType _positionType;
    ModeA modeA = new ModeA();
    ModeB modeB = new ModeB();


    float gravityX;
    float gravityY;
    /** Mode
     * @js cc.ParticleSystem.MODE_GRAVITY;
     */


    enum Mode
    {
        GRAVITY,
        RADIUS,
    }

    /** PositionType
     Possible types of particle positions.
     * @js cc.ParticleSystem.TYPE_FREE
     */
    enum PositionType
    {
        FREE, /** Living particles are attached to the world and are unaffected by emitter repositioning. */

        RELATIVE, /** Living particles are attached to the world but will follow the emitter repositioning.
     Use case: Attach an emitter to an sprite, and you want that the emitter follows the sprite.*/

        GROUPED, /** Living particles are attached to the emitter and are translated along with it. */

    }
    /** The Particle emitter lives forever. */
    final static int DURATION_INFINITY = -1;
    /** The starting diameter of the particle is equal to the ending diameter. */
    final static int  START_SIZE_EQUAL_TO_END_SIZE = -1;
    /** The starting radius of the particle is equal to the ending radius. */
    final static int     START_RADIUS_EQUAL_TO_END_RADIUS = -1;

    // Different modes
    //! Mode A:Gravity + Tangential Accel + Radial Accel
    class ModeA {
        /** Gravity value. Only available in 'Gravity' mode. */
        Vec2 gravity = new Vec2(0f,0f);
        /** speed of each particle. Only available in 'Gravity' mode.  */
        float speed;
        /** speed variance of each particle. Only available in 'Gravity' mode. */
        float speedVar;
        /** tangential acceleration of each particle. Only available in 'Gravity' mode. */
        float tangentialAccel;
        /** tangential acceleration variance of each particle. Only available in 'Gravity' mode. */
        float tangentialAccelVar;
        /** radial acceleration of each particle. Only available in 'Gravity' mode. */
        float radialAccel;
        /** radial acceleration variance of each particle. Only available in 'Gravity' mode. */
        float radialAccelVar;
        /** set the rotation of each particle to its direction Only available in 'Gravity' mode. */
        boolean rotationIsDir;
    }
    //! Mode B: circular movement (gravity, radial accel and tangential accel don't are not used in this mode)
    class ModeB {
        /** The starting radius of the particles. Only available in 'Radius' mode. */
        float startRadius;
        /** The starting radius variance of the particles. Only available in 'Radius' mode. */
        float startRadiusVar;
        /** The ending radius of the particles. Only available in 'Radius' mode. */
        float endRadius;
        /** The ending radius variance of the particles. Only available in 'Radius' mode. */
        float endRadiusVar;
        /** Number of degrees to rotate a particle around the source pos per second. Only available in 'Radius' mode. */
        float rotatePerSecond;
        /** Variance in degrees for rotatePerSecond. Only available in 'Radius' mode. */
        float rotatePerSecondVar;
    }


    /** @def CC_RADIANS_TO_DEGREES
    converts radians to degrees
     */
    public static float CC_RADIANS_TO_DEGREES(float angle) {
        return angle * 57.29577951f;

    }
    /** @def CC_DEGREES_TO_RADIANS
    converts degrees to radians
     */
    public static float CC_DEGREES_TO_RADIANS(float angle) {
        return angle * 0.01745329252f;
    }

    static Random _random = new Random();

    public static float CCRANDOM_MINUS1_1() {
        return _random.nextFloat() * 2 - 1; // rng.nextDouble() is between 0 and 1
    }

    public static float clampf(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

}
