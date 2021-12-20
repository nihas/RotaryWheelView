package com.nihas.rotarywheel

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.view.View.OnTouchListener
import androidx.appcompat.widget.PopupMenu
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringSystem
import com.nihas.rotarywheel.utils.BalloonPopup
import java.util.*
import kotlin.collections.ArrayList

class ControllerViewSimple(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int): View(context,attrs,defStyleAttr,defStyleRes) {
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : this(context, attrs, defStyle,0){
        init(attrs)
    }
    constructor(context: Context, attrs: AttributeSet): this(context,attrs,0,0){ init(attrs) }
    constructor(context: Context): this(context,null,0,0){ init(null) }

    val SWIPEDIRECTION_NONE = 0
    val SWIPEDIRECTION_VERTICAL = 1
    val SWIPEDIRECTION_HORIZONTAL = 2
    val SWIPEDIRECTION_HORIZONTALVERTICAL = 3
    val SWIPEDIRECTION_CIRCULAR = 4

    val ONCLICK_NONE = 0
    val ONCLICK_NEXT = 1
    val ONCLICK_PREV = 2
    val ONCLICK_RESET = 3
    val ONCLICK_MENU = 4
    val ONCLICK_USER = 5

    val BALLONANIMATION_POP = 0
    val BALLONANIMATION_SCALE = 1
    val BALLONANIMATION_FADE = 2

    private var paint: Paint? = null
    private var ctx: Context? = null
    private var externalRadius = 0f
    private  var knobRadius:Float = 0f
    private  var centerX:Float = 0f
    private  var centerY:Float = 0f
    var springSystem: SpringSystem? = null
    var spring: Spring? = null
    private var currentAngle = 0.0
    private var previousState: Int = 0
        get()= defaultState
    private var knobDrawable: Drawable? = null
    private var balloonPopup: BalloonPopup? = null


    // default values
    private var numberOfStates = 6
    private var defaultState = 0
    private var borderWidth = 2
    private var borderColor = Color.BLACK
    private var indicatorWidth = 6
    private var indicatorColor = Color.BLACK
    private var indicatorRelativeLength = 0.35f
    private var circularIndicatorRelativeRadius = 0.0f
    private var circularIndicatorRelativePosition = 0.7f
    private var circularIndicatorColor = Color.BLACK
    private var knobColor = Color.LTGRAY
    private var knobRelativeRadius = 0.8f
    private var knobCenterRelativeRadius = 0.45f
    private var knobCenterColor = Color.DKGRAY
    private var controllerEnabled = true
    private var currentState = defaultState // can be negative and override expected limits

    private var actualState = currentState // currentState, modded to the expected limits

    private var animation = true
    private var animationSpeed = 10f
    private var animationBounciness = 40f
    private var stateMarkersWidth = 2
    private var stateMarkersColor = Color.BLACK
    private var selectedStateMarkerColor = Color.YELLOW
    private var selectedStateMarkerContinuous = false
    private var stateMarkersRelativeLength = 0.06f
    private var swipeDirection = 4 // circular  (before it was horizontal)

    private var swipeSensibilityPixels = 100
    private var swipeX = 0
    private  var swipeY:Int = 0 // used for swipe management

    var swipeing = false // used for swipe / click management

    private var freeRotation = true
    private var minAngle = 0f
    private var maxAngle = 360f
    private var stateMarkersAccentWidth = 3
    private var stateMarkersAccentColor = Color.BLACK
    private var stateMarkersAccentRelativeLength = 0.11f
    private var stateMarkersAccentPeriodicity = 0 // 0 = off

    private var knobDrawableRes = 0
    private var knobDrawableRotates = true
    private var showBalloonValues = false
    private var balloonValuesTimeToLive = 400
    private var balloonValuesRelativePosition = 1.3f
    private var balloonValuesTextSize = 9f
    private var balloonValuesAnimation: Int = BALLONANIMATION_POP
    private var balloonValuesArray: Array<CharSequence>? = null
    private var balloonValuesSlightlyTransparent = true
    private var clickBehaviour: Int = ONCLICK_NEXT // next

    var dummyValuesArr = ArrayList<String>()

    private var userRunnable: Runnable? = null

    init {
        for(i in -100..100){
            this.dummyValuesArr.add(i.toString())
        }
    }


    // overrides

    // overrides
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var widthSize = MeasureSpec.getSize(widthMeasureSpec)
        var heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val r = Resources.getSystem()
        if (widthMode == MeasureSpec.UNSPECIFIED || widthMode == MeasureSpec.AT_MOST) {
            widthSize =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, r.displayMetrics)
                    .toInt()
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY)
        }
        if (heightMode == MeasureSpec.UNSPECIFIED || heightSize == MeasureSpec.AT_MOST) {
            heightSize =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30f, r.displayMetrics)
                    .toInt()
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val width = width
        val height = height
        externalRadius = Math.min(width, height) * 0.5f
        knobRadius = externalRadius * knobRelativeRadius
        centerX = (width / 2).toFloat()
        centerY = (height / 2).toFloat()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paintKnob(canvas)
        paintMarkers(canvas)
        paintIndicator(canvas)
        paintCircularIndicator(canvas)
        paintKnobCenter(canvas)
        paintKnobBorder(canvas)
        displayBalloons()
    }

    fun paintKnob(canvas: Canvas) {
        if (knobDrawableRes != 0 && knobDrawable != null) {
            knobDrawable?.setBounds(
                (centerX - knobRadius).toInt(),
                (centerY - knobRadius).toInt(),
                (centerX + knobRadius).toInt(),
                (centerY + knobRadius).toInt()
            )
            if (knobDrawableRotates) {
                canvas.save()
                canvas.rotate((-Math.toDegrees(Math.PI + currentAngle)).toFloat(), centerX, centerY)
                knobDrawable?.draw(canvas)
                canvas.restore()
            } else knobDrawable?.draw(canvas)
        } else {
            paint?.setColor(knobColor)
            paint?.setStyle(Paint.Style.FILL)
            paint?.let { canvas.drawCircle(centerX, centerY, knobRadius, it) }
        }
    }

    fun paintKnobBorder(canvas: Canvas) {
        if (borderWidth == 0) return
        paint?.setColor(borderColor)
        paint?.setStyle(Paint.Style.STROKE)
        paint?.setStrokeWidth(borderWidth.toFloat())
        paint?.let { canvas.drawCircle(centerX, centerY, knobRadius, it) }
    }

    fun paintKnobCenter(canvas: Canvas) {
        if (knobDrawableRes != 0 && knobDrawable != null) return
        if (knobCenterRelativeRadius == 0f) return
        paint?.setColor(knobCenterColor)
        paint?.setStyle(Paint.Style.FILL)
        paint?.let {
            canvas.drawCircle(centerX, centerY, knobCenterRelativeRadius * knobRadius,
                it
            )
        }
    }

    fun normalizeAngle(angle: Double): Double {
        var angle = angle
        while (angle < 0) angle += Math.PI * 2
        while (angle >= Math.PI * 2) angle -= Math.PI * 2
        return angle
    }

    fun calcAngle(position: Int): Double {
        val min = Math.toRadians(minAngle.toDouble())
        val max = Math.toRadians(maxAngle.toDouble() - 0.0001)
        val range = max - min
        if (numberOfStates <= 1) return 0.0
        var singleStepAngle: Double = range / (numberOfStates - 1)
        if (Math.PI * 2 - range < singleStepAngle) singleStepAngle = range / numberOfStates
        return normalizeAngle(Math.PI - min - position * singleStepAngle)

        // return Math.PI - position * (2 * Math.PI / numberOfStates);
    }

    fun setIndicatorAngleWithDirection() {
        val angleCurr = normalizeAngle(spring?.currentValue ?:0.0)
        var angleNew = calcAngle(actualState)
        if (freeRotation) {
            if (angleCurr > angleNew && angleCurr - angleNew > Math.PI) angleNew += Math.PI * 2 else if (angleCurr < angleNew && angleNew - angleCurr > Math.PI) angleNew -= Math.PI * 2
        }
        spring?.setCurrentValue(angleCurr)
        spring?.setEndValue(angleNew)
    }

    fun paintIndicator(canvas: Canvas) {
        if (indicatorWidth == 0) return
        if (indicatorRelativeLength == 0.0f) return
        paint?.setColor(indicatorColor)
        paint?.setStrokeWidth(indicatorWidth.toFloat())
        val startX =
            centerX + (knobRadius * (1 - indicatorRelativeLength) * Math.sin(currentAngle))
        val startY =
            centerY + (knobRadius * (1 - indicatorRelativeLength) * Math.cos(currentAngle))
        val endX = centerX + (knobRadius * Math.sin(currentAngle)).toFloat()
        val endY = centerY + (knobRadius * Math.cos(currentAngle)).toFloat()
        paint?.let { canvas.drawLine(startX.toFloat(), startY.toFloat(), endX, endY, it) }
    }

    fun paintCircularIndicator(canvas: Canvas) {
        if (circularIndicatorRelativeRadius == 0.0f) return
        paint?.setColor(circularIndicatorColor)
        paint?.setStrokeWidth(0f)
        paint?.setStyle(Paint.Style.FILL)
        val posX =
            centerX + (externalRadius * circularIndicatorRelativePosition * Math.sin(currentAngle)).toFloat()
        val posY =
            centerY + (externalRadius * circularIndicatorRelativePosition * Math.cos(currentAngle)).toFloat()
        paint?.let {
            canvas.drawCircle(posX, posY, externalRadius * circularIndicatorRelativeRadius,
                it
            )
        }
    }

    fun paintMarkers(canvas: Canvas) {
        if ((stateMarkersRelativeLength == 0f || stateMarkersWidth == 0) && (stateMarkersAccentRelativeLength == 0f || stateMarkersAccentWidth == 0)) return
        for (w in 0 until numberOfStates) {
            var big = false
            var selected = false
            if (stateMarkersAccentPeriodicity != 0) big = w % stateMarkersAccentPeriodicity == 0
            selected = w == actualState || w <= actualState && selectedStateMarkerContinuous
            paint?.setStrokeWidth(if (big) stateMarkersAccentWidth.toFloat() else stateMarkersWidth.toFloat())
            val angle = calcAngle(w)
            val startX =
                centerX + (externalRadius * (1 - if (big) stateMarkersAccentRelativeLength else stateMarkersRelativeLength) * Math.sin(
                    angle
                )).toFloat()
            val startY =
                centerY + (externalRadius * (1 - if (big) stateMarkersAccentRelativeLength else stateMarkersRelativeLength) * Math.cos(
                    angle
                )).toFloat()
            val endX = centerX + (externalRadius * Math.sin(angle)).toFloat()
            val endY = centerY + (externalRadius * Math.cos(angle)).toFloat()
            paint?.setColor(if (selected) selectedStateMarkerColor else if (big) stateMarkersAccentColor else stateMarkersColor)
            paint?.let { canvas.drawLine(startX, startY, endX, endY, it) }
        }
    }

    fun balloonsX(): Int {
        return (centerX + (externalRadius * balloonValuesRelativePosition * Math.sin(currentAngle)).toFloat()).toInt()
    }

    fun balloonsY(): Int {
        return (centerY + (externalRadius * balloonValuesRelativePosition * Math.cos(currentAngle)).toFloat()).toInt()
    }

    fun balloonText(): String? {
        println("================ActualState==========$balloonValuesArray")
        return if (balloonValuesArray == null) Integer.toString(actualState) else balloonValuesArray?.get(
            actualState
        ).toString()
    }

    fun setBalloonText(){

    }

    fun displayBalloons() {
        if (!showBalloonValues) return
        if (balloonPopup == null || !balloonPopup?.isShowing!!) balloonPopup =
            BalloonPopup.Builder(ctx, this)
                .text(balloonText())
                .gravity(BalloonPopup.BalloonGravity.halftop_halfleft)
                .offsetX(balloonsX())
                .offsetY(balloonsY())
                .textSize(balloonValuesTextSize.toInt())
                .shape(BalloonPopup.BalloonShape.rounded_square)
                .timeToLive(balloonValuesTimeToLive)
                .animation(getBalloonAnimation())
                .stayWithinScreenBounds(true)
                .show() else {
            balloonPopup?.updateOffset(balloonsX(), balloonsY(), true)
            balloonPopup?.updateText(balloonText(), true)
            balloonPopup?.updateTextSize(
                balloonValuesTextSize.toInt(),
                true
            ) // solo l'ultimo richiede l'aggiornamento del timer?
        }
    }

    fun getBalloonAnimation(): BalloonPopup.BalloonAnimation? {
        return if (balloonValuesAnimation == 0 && balloonValuesSlightlyTransparent) BalloonPopup.BalloonAnimation.fade75_and_pop else if (balloonValuesAnimation == 0) BalloonPopup.BalloonAnimation.fade_and_pop else if (balloonValuesAnimation == 1 && balloonValuesSlightlyTransparent) BalloonPopup.BalloonAnimation.fade75_and_scale else if (balloonValuesAnimation == 1) BalloonPopup.BalloonAnimation.fade_and_scale else if (balloonValuesAnimation == 2 && balloonValuesSlightlyTransparent) BalloonPopup.BalloonAnimation.fade75 else BalloonPopup.BalloonAnimation.fade
    }


    // initialize
    fun init(attrs: AttributeSet?) {
        ctx = context
        loadAttributes(attrs)
        initTools()
        initDrawables()
        initBalloons()
        initListeners()
        initStatus()
    }

    fun initTools() {
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint?.setStrokeCap(Paint.Cap.ROUND)
        springSystem = SpringSystem.create()
        spring = springSystem?.createSpring()
//        spring?.setSpringConfig(
//            SpringConfig.fromBouncinessAndSpeed(
//                animationSpeed.toDouble(),
//                animationBounciness.toDouble()
//            )
//        )
        spring?.isOvershootClampingEnabled = false
    }

    fun initDrawables() {
        if (knobDrawableRes != 0) {
            knobDrawable = resources.getDrawable(knobDrawableRes)
        }
    }

    fun loadAttributes(attrs: AttributeSet?) {
        if (attrs == null) return
        val typedArray: TypedArray = ctx!!.obtainStyledAttributes(attrs, R.styleable.Knob)
        numberOfStates = typedArray.getInt(R.styleable.Knob_kNumberOfStates, numberOfStates)
        defaultState = typedArray.getInt(R.styleable.Knob_kDefaultState, defaultState)
        borderWidth = typedArray.getDimensionPixelSize(R.styleable.Knob_kBorderWidth, borderWidth)
        borderColor = typedArray.getColor(R.styleable.Knob_kBorderColor, borderColor)
        indicatorWidth =
            typedArray.getDimensionPixelSize(R.styleable.Knob_kIndicatorWidth, indicatorWidth)
        indicatorColor = typedArray.getColor(R.styleable.Knob_kIndicatorColor, indicatorColor)
        indicatorRelativeLength =
            typedArray.getFloat(R.styleable.Knob_kIndicatorRelativeLength, indicatorRelativeLength)
        circularIndicatorRelativeRadius = typedArray.getFloat(
            R.styleable.Knob_kCircularIndicatorRelativeRadius,
            circularIndicatorRelativeRadius
        )
        circularIndicatorRelativePosition = typedArray.getFloat(
            R.styleable.Knob_kCircularIndicatorRelativePosition,
            circularIndicatorRelativePosition
        )
        circularIndicatorColor =
            typedArray.getColor(R.styleable.Knob_kCircularIndicatorColor, circularIndicatorColor)
        knobColor = typedArray.getColor(R.styleable.Knob_kKnobColor, knobColor)
        knobRelativeRadius =
            typedArray.getFloat(R.styleable.Knob_kKnobRelativeRadius, knobRelativeRadius)
        knobCenterRelativeRadius = typedArray.getFloat(
            R.styleable.Knob_kKnobCenterRelativeRadius,
            knobCenterRelativeRadius
        )
        knobCenterColor = typedArray.getColor(R.styleable.Knob_kKnobCenterColor, knobCenterColor)
        knobDrawableRes = typedArray.getResourceId(R.styleable.Knob_kKnobDrawable, knobDrawableRes)
        knobDrawableRotates =
            typedArray.getBoolean(R.styleable.Knob_kKnobDrawableRotates, knobDrawableRotates)
        stateMarkersWidth =
            typedArray.getDimensionPixelSize(R.styleable.Knob_kStateMarkersWidth, stateMarkersWidth)
        stateMarkersColor =
            typedArray.getColor(R.styleable.Knob_kStateMarkersColor, stateMarkersColor)
        selectedStateMarkerColor = typedArray.getColor(
            R.styleable.Knob_kSelectedStateMarkerColor,
            selectedStateMarkerColor
        )
        stateMarkersRelativeLength = typedArray.getFloat(
            R.styleable.Knob_kStateMarkersRelativeLength,
            stateMarkersRelativeLength
        )
        selectedStateMarkerContinuous = typedArray.getBoolean(
            R.styleable.Knob_kSelectedStateMarkerContinuous,
            selectedStateMarkerContinuous
        )
        animation = typedArray.getBoolean(R.styleable.Knob_kAnimation, animation)
        animationSpeed = typedArray.getFloat(R.styleable.Knob_kAnimationSpeed, animationSpeed)
        animationBounciness =
            typedArray.getFloat(R.styleable.Knob_kAnimationBounciness, animationBounciness)
        swipeDirection = swipeAttrToInt(typedArray.getString(R.styleable.Knob_kSwipe))
        swipeSensibilityPixels =
            typedArray.getInt(R.styleable.Knob_kSwipeSensitivityPixels, swipeSensibilityPixels)
        freeRotation = typedArray.getBoolean(R.styleable.Knob_kFreeRotation, freeRotation)
        minAngle = typedArray.getFloat(R.styleable.Knob_kMinAngle, minAngle)
        maxAngle = typedArray.getFloat(R.styleable.Knob_kMaxAngle, maxAngle)
        stateMarkersAccentWidth = typedArray.getDimensionPixelSize(
            R.styleable.Knob_kStateMarkersAccentWidth,
            stateMarkersAccentWidth
        )
        stateMarkersAccentColor =
            typedArray.getColor(R.styleable.Knob_kStateMarkersAccentColor, stateMarkersAccentColor)
        stateMarkersAccentRelativeLength = typedArray.getFloat(
            R.styleable.Knob_kStateMarkersAccentRelativeLength,
            stateMarkersAccentRelativeLength
        )
        stateMarkersAccentPeriodicity = typedArray.getInt(
            R.styleable.Knob_kStateMarkersAccentPeriodicity,
            stateMarkersAccentPeriodicity
        )
        showBalloonValues =
            typedArray.getBoolean(R.styleable.Knob_kShowBalloonValues, showBalloonValues)
        balloonValuesTimeToLive =
            typedArray.getInt(R.styleable.Knob_kBalloonValuesTimeToLive, balloonValuesTimeToLive)
        balloonValuesRelativePosition = typedArray.getFloat(
            R.styleable.Knob_kBalloonValuesRelativePosition,
            balloonValuesRelativePosition
        )
        balloonValuesTextSize =
            typedArray.getDimension(R.styleable.Knob_kBalloonValuesTextSize, balloonValuesTextSize)
        balloonValuesAnimation =
            balloonAnimationAttrToInt(typedArray.getString(R.styleable.Knob_kBalloonValuesAnimation))
        balloonValuesArray = typedArray.getTextArray(R.styleable.Knob_kBalloonValuesArray)
        balloonValuesArray = dummyValuesArr.toTypedArray()
        balloonValuesSlightlyTransparent = typedArray.getBoolean(
            R.styleable.Knob_kBalloonValuesSlightlyTransparent,
            balloonValuesSlightlyTransparent
        )
        clickBehaviour = clickAttrToInt(typedArray.getString(R.styleable.Knob_kClickBehaviour))
        controllerEnabled = typedArray.getBoolean(R.styleable.Knob_kEnabled, controllerEnabled)
        typedArray.recycle()
    }

    fun swipeAttrToInt(s: String?): Int {
        if (s == null) return SWIPEDIRECTION_CIRCULAR
        return if (s == "0") SWIPEDIRECTION_NONE else if (s == "1") SWIPEDIRECTION_VERTICAL // vertical
        else if (s == "2") SWIPEDIRECTION_HORIZONTAL // horizontal
        else if (s == "3") SWIPEDIRECTION_HORIZONTALVERTICAL // both
        else if (s == "4") SWIPEDIRECTION_CIRCULAR // default  - circular
        else SWIPEDIRECTION_CIRCULAR
    }

    fun clickAttrToInt(s: String?): Int {
        if (s == null) return ONCLICK_NEXT
        return if (s == "0") ONCLICK_NONE else if (s == "1") ONCLICK_NEXT // default - next
        else if (s == "2") ONCLICK_PREV // prev
        else if (s == "3") ONCLICK_RESET // reset
        else if (s == "4") ONCLICK_MENU // menu
        else if (s == "5") ONCLICK_USER // menu
        else ONCLICK_NEXT
    }

    fun balloonAnimationAttrToInt(s: String?): Int {
        if (s == null) return BALLONANIMATION_POP
        return if (s == "0") BALLONANIMATION_POP // pop
        else if (s == "1") BALLONANIMATION_SCALE // scale
        else if (s == "2") BALLONANIMATION_FADE // fade
        else BALLONANIMATION_POP
    }

    private fun disallowParentToHandleTouchEvents() {
        val parent = parent
        parent?.requestDisallowInterceptTouchEvent(true)
    }

    fun clickMe(view: View?) {
        when (clickBehaviour) {
            ONCLICK_NONE -> {
            }
            ONCLICK_NEXT -> toggle(animation)
            ONCLICK_PREV -> inverseToggle(animation)
            ONCLICK_RESET -> revertToDefault(animation)
            ONCLICK_MENU -> createPopupMenu(view)
            ONCLICK_USER -> runUserBehaviour()
        }
    }

    fun initListeners() {
        setOnClickListener(OnClickListener { view ->
            if (!controllerEnabled) return@OnClickListener
            clickMe(view)
        })
        setOnTouchListener(OnTouchListener { view, motionEvent ->
            if (!controllerEnabled) return@OnTouchListener false
            if (swipeDirection == SWIPEDIRECTION_NONE) {
                toggle(animation)
                return@OnTouchListener false
            }
            val action = motionEvent.action
            if (swipeDirection == SWIPEDIRECTION_VERTICAL) {  // vertical
                val y = motionEvent.y.toInt()
                if (action == MotionEvent.ACTION_DOWN) {
                    swipeY = y
                    swipeing = false
                    disallowParentToHandleTouchEvents() // needed when Knob's parent is a ScrollView
                } else if (action == MotionEvent.ACTION_MOVE) {
                    if (y - swipeY > swipeSensibilityPixels) {
                        swipeY = y
                        swipeing = true
                        decreaseValue()
                        return@OnTouchListener true
                    } else if (swipeY - y > swipeSensibilityPixels) {
                        swipeY = y
                        swipeing = true
                        increaseValue()
                        return@OnTouchListener true
                    }
                } else if (action == MotionEvent.ACTION_UP) {
                    if (!swipeing) clickMe(view) // click
                    return@OnTouchListener true
                }
                return@OnTouchListener false
            } else if (swipeDirection == SWIPEDIRECTION_HORIZONTAL) {  // horizontal
                val x = motionEvent.x.toInt()
                if (action == MotionEvent.ACTION_DOWN) {
                    swipeX = x
                    swipeing = false
                    disallowParentToHandleTouchEvents() // needed when Knob's parent is a ScrollView
                } else if (action == MotionEvent.ACTION_MOVE) {
                    if (x - swipeX > swipeSensibilityPixels) {
                        swipeX = x
                        swipeing = true
                        increaseValue()
                        return@OnTouchListener true
                    } else if (swipeX - x > swipeSensibilityPixels) {
                        swipeX = x
                        swipeing = true
                        decreaseValue()
                        return@OnTouchListener true
                    }
                } else if (action == MotionEvent.ACTION_UP) {
                    if (!swipeing) clickMe(view) // click
                    return@OnTouchListener true
                }
                return@OnTouchListener false
            } else if (swipeDirection == SWIPEDIRECTION_HORIZONTALVERTICAL) {  // both
                val x = motionEvent.x.toInt()
                val y = motionEvent.y.toInt()
                if (action == MotionEvent.ACTION_DOWN) {
                    swipeX = x
                    swipeY = y
                    swipeing = false
                    disallowParentToHandleTouchEvents() // needed when Knob's parent is a ScrollView
                } else if (action == MotionEvent.ACTION_MOVE) {
                    if (x - swipeX > swipeSensibilityPixels || swipeY - y > swipeSensibilityPixels) {
                        swipeX = x
                        swipeY = y
                        swipeing = true
                        increaseValue()
                        return@OnTouchListener true
                    } else if (swipeX - x > swipeSensibilityPixels || y - swipeY > swipeSensibilityPixels) {
                        swipeX = x
                        swipeY = y
                        swipeing = true
                        decreaseValue()
                        return@OnTouchListener true
                    }
                } else if (action == MotionEvent.ACTION_UP) {
                    if (!swipeing) clickMe(view) // click
                    return@OnTouchListener true
                }
                return@OnTouchListener false
            } else if (swipeDirection == SWIPEDIRECTION_CIRCULAR) { // circular
                val x = motionEvent.x.toInt()
                val y = motionEvent.y.toInt()
                if (action == MotionEvent.ACTION_DOWN) {
                    swipeing = false
                    disallowParentToHandleTouchEvents() // needed when Knob's parent is a ScrollView
                } else if (action == MotionEvent.ACTION_MOVE) {
                    val angle = Math.atan2(
                        (y - centerY).toDouble(),
                        (x - centerX).toDouble()
                    )
                    swipeing = true
                    setValueByAngle(angle, animation)
                    return@OnTouchListener true
                } else if (action == MotionEvent.ACTION_UP) {
                    if (!swipeing) clickMe(view) // click
                    return@OnTouchListener true
                }
                return@OnTouchListener false
            }
            false
        })
        spring?.addListener(object : SimpleSpringListener() {
            override fun onSpringUpdate(spring: Spring) {
                currentAngle = spring.getCurrentValue()
                postInvalidate()
            }
        })
    }

    fun createPopupMenu(view: View?) {
        val mPopupMenu = PopupMenu(context, view!!)
        if (balloonValuesArray == null) for (w in 0 until numberOfStates) mPopupMenu.menu.add(
            Menu.NONE,
            w + 1,
            w + 1,
            Integer.toString(w)
        ) else for (w in 0 until numberOfStates) mPopupMenu.menu.add(
            Menu.NONE, w + 1, w + 1, balloonValuesArray?.get(w).toString()
        )
        mPopupMenu.setOnMenuItemClickListener { item ->
            val i = item.itemId - 1
            setState(i)
            true
        }
        mPopupMenu.show()
    }

    fun initStatus() {
        currentState = defaultState
        previousState = defaultState
        calcActualState()
        currentAngle = calcAngle(currentState)
        spring?.setCurrentValue(currentAngle)
    }

    fun initBalloons() {}

    // behaviour

    // behaviour
    fun toggle(animate: Boolean) {
        increaseValue(animate)
    }

    fun toggle() {
        toggle(animation)
    }

    fun inverseToggle(animate: Boolean) {
        decreaseValue(animate)
    }

    fun inverseToggle() {
        inverseToggle(animation)
    }

    fun revertToDefault(animate: Boolean) {
        setState(defaultState, animate)
    }

    fun revertToDefault() {
        revertToDefault(animation)
    }

    private fun calcActualState() {
        actualState = currentState % numberOfStates
        if (actualState < 0) actualState += numberOfStates
    }

    fun increaseValue(animate: Boolean) {
        previousState = currentState
        currentState = currentState + 1 // % numberOfStates;
        if (!freeRotation && currentState >= numberOfStates) currentState = numberOfStates - 1
        calcActualState()
        if (listener != null) listener!!.onState(actualState)
        takeEffect(animate)
    }

    fun increaseValue() {
        increaseValue(animation)
    }

    fun decreaseValue(animate: Boolean) {
        previousState = currentState
        currentState = currentState - 1 // % numberOfStates;
        if (!freeRotation && currentState < 0) currentState = 0
        calcActualState()
        if (listener != null) listener!!.onState(actualState)
        takeEffect(animate)
    }

    fun decreaseValue() {
        decreaseValue(animation)
    }

    fun setValueByAngle(
        angle: Double,
        animate: Boolean
    ) {  // sets the value of the knob given an angle instead of a state
        var angle = angle
        if (numberOfStates <= 1) return
        previousState = currentState
        var min = Math.toRadians(minAngle.toDouble())
        var max = Math.toRadians(maxAngle.toDouble() - 0.0001)
        val range = max - min
        var singleStepAngle: Double = range / numberOfStates
        if (Math.PI * 2 - range < singleStepAngle) singleStepAngle = range / numberOfStates
        min = normalizeAngle(min)
        while (min > max) max += 2 * Math.PI // both min and max are positive and in the correct order.
        angle = normalizeAngle(angle + Math.PI / 2)
        while (angle < min) angle += 2 * Math.PI // set angle after minangle
        if (angle > max) { // if angle is out of range because the range is limited set to the closer limit
            angle = if (angle - max > min - angle + Math.PI * 2) min else max
        }
        currentState = ((angle - min) / singleStepAngle).toInt() // calculate value
        if (!freeRotation && Math.abs(currentState - previousState) == numberOfStates - 1) // manage free rotation
            currentState = previousState
        calcActualState()
        if (listener != null) listener!!.onState(actualState)
        takeEffect(animate)
    }

    private fun takeEffect(animate: Boolean) {
        if (animate) {
            setIndicatorAngleWithDirection()
        } else {
            spring?.currentValue = calcAngle(actualState)
        }
        postInvalidate()
    }

    // public listener interface

    // public listener interface
    private var listener: OnStateChanged? = null

    interface OnStateChanged {
        fun onState(state: Int)
    }

    fun setOnStateChanged(onStateChanged: OnStateChanged?) {
        listener = onStateChanged
    }

    // methods

    // methods
    fun setState(newState: Int) {
        setState(newState, animation)
    }

    fun setState(newState: Int, animate: Boolean) {
        forceState(newState, animate)
        if (listener != null) listener!!.onState(currentState)
    }

    fun forceState(newState: Int) {
        forceState(newState, animation)
    }

    fun forceState(newState: Int, animate: Boolean) {
        previousState = currentState
        currentState = newState
        calcActualState()
        takeEffect(animate)
    }

    fun getState(): Int {
        return actualState
    }

    // getters and setters


    fun setUserBehaviour(userRunnable: Runnable) {
        // when "user" click behaviour is selected
        this.userRunnable = userRunnable
    }

    fun runUserBehaviour() {   // to be initialized with setUserBehaviour()
        if (userRunnable == null) return
        userRunnable?.run()
    }
}