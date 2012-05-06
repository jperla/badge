/*==============================================================================
            Copyright (c) 2012 QUALCOMM Austria Research Center GmbH.
            All Rights Reserved.
            Qualcomm Confidential and Proprietary
            
@file 
    FrameMarkers.cpp

@brief
    Sample for FrameMarkers

==============================================================================*/


#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include <QCAR/QCAR.h>
#include <QCAR/CameraDevice.h>
#include <QCAR/Renderer.h>
#include <QCAR/VideoBackgroundConfig.h>
#include <QCAR/Trackable.h>
#include <QCAR/Tool.h>
#include <QCAR/MarkerTracker.h>
#include <QCAR/TrackerManager.h>
#include <QCAR/CameraCalibration.h>
#include <QCAR/Marker.h>

#include "SampleUtils.h"
#include "Texture.h"
#include "CubeShaders.h"
#include "Q_object.h"
#include "C_object.h"
#include "A_object.h"
#include "R_object.h"

#ifdef __cplusplus
extern "C"
{
#endif

// Textures:
int textureCount                = 0;
Texture** textures              = 0;

// OpenGL ES 2.0 specific:
unsigned int shaderProgramID    = 0;
GLint vertexHandle              = 0;
GLint normalHandle              = 0;
GLint textureCoordHandle        = 0;
GLint mvpMatrixHandle           = 0;

// Screen dimensions:
unsigned int screenWidth        = 0;
unsigned int screenHeight       = 0;

// Indicates whether screen is in portrait (true) or landscape (false) mode
bool isActivityInPortraitMode   = false;

// The projection matrix used for rendering virtual objects:
QCAR::Matrix44F projectionMatrix;

// Constants:
static const float kLetterScale        = 25.0f;
static const float kLetterTranslate    = 25.0f;


JNIEXPORT void JNICALL
Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkers_setActivityPortraitMode(JNIEnv *, jobject, jboolean isPortrait)
{
    isActivityInPortraitMode = isPortrait;
}


JNIEXPORT int JNICALL
Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkers_initTracker(JNIEnv *, jobject)
{
    LOG("Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkers_initTracker");
    
    // Initialize the marker tracker:
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::Tracker* trackerBase = trackerManager.initTracker(QCAR::Tracker::MARKER_TRACKER);
    QCAR::MarkerTracker* markerTracker = static_cast<QCAR::MarkerTracker*>(trackerBase);
    if (markerTracker == NULL)
    {
        LOG("Failed to initialize MarkerTracker.");
        return 0;
    }
    LOG("Successfully initialized MarkerTracker.");

    // Create frame markers:
    if (!markerTracker->createFrameMarker(0, "MarkerQ", QCAR::Vec2F(50,50)) ||
        !markerTracker->createFrameMarker(1, "MarkerC", QCAR::Vec2F(50,50)) ||
        !markerTracker->createFrameMarker(2, "MarkerA", QCAR::Vec2F(50,50)) ||
        !markerTracker->createFrameMarker(3, "MarkerR", QCAR::Vec2F(50,50)))
    {
        LOG("Failed to create frame markers.");
        return 0;
    }
    LOG("Successfully created frame markers.");

    return 1;
}


JNIEXPORT void JNICALL
    Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkers_deinitTracker(JNIEnv *, jobject)
{
    LOG("Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkers_deinitTracker");

    // Deinit the marker tracker, this will destroy all created frame markers:
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    trackerManager.deinitTracker(QCAR::Tracker::MARKER_TRACKER);
}


JNIEXPORT int JNICALL
Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkersRenderer_renderFrame(JNIEnv *, jobject)
{
    //LOG("Java_com_qualcomm_QCARSamples_FrameMarkers_GLRenderer_renderFrame");
 
    // Clear color and depth buffer 
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    // Get the state from QCAR and mark the beginning of a rendering section
    QCAR::State state = QCAR::Renderer::getInstance().begin();

    // Explicitly render the Video Background
    QCAR::Renderer::getInstance().drawVideoBackground();
    
    glEnable(GL_DEPTH_TEST);
    glEnable(GL_CULL_FACE);

    int markerId = -1;

    // Did we find any trackables this frame?
    for(int tIdx = 0; tIdx < state.getNumActiveTrackables(); tIdx++)
    {
        // Get the trackable:
        const QCAR::Trackable* trackable = state.getActiveTrackable(tIdx);
        QCAR::Matrix44F modelViewMatrix =
            QCAR::Tool::convertPose2GLMatrix(trackable->getPose());        
      
        // Choose the texture based on the target name:
        int textureIndex = 0;
        
        // Check the type of the trackable:
        assert(trackable->getType() == QCAR::Trackable::MARKER);
        const QCAR::Marker* marker = static_cast<const QCAR::Marker*>(trackable);

        textureIndex = marker->getMarkerId();

        assert(textureIndex < textureCount);
        const Texture* const thisTexture = textures[textureIndex];

        // Select which model to draw:
        const GLvoid* vertices = 0;
        const GLvoid* normals = 0;
        const GLvoid* indices = 0;
        const GLvoid* texCoords = 0;
        int numIndices = 0;

        if (markerId < 0) {
            if (textureIndex >= 0 && textureIndex < 4) {
                markerId = textureIndex;
            }
        }

        switch (marker->getMarkerId())
        {
        case 0:
            vertices = &QobjectVertices[0];
            normals = &QobjectNormals[0];
            indices = &QobjectIndices[0];
            texCoords = &QobjectTexCoords[0];
            numIndices = NUM_Q_OBJECT_INDEX;
            LOG("----- IN JNI ----- detected marker ID 0");
        
            break;
        case 1:
            vertices = &CobjectVertices[0];
            normals = &CobjectNormals[0];
            indices = &CobjectIndices[0];
            texCoords = &CobjectTexCoords[0];
            numIndices = NUM_C_OBJECT_INDEX;
            LOG("----- IN JNI ----- detected marker ID 1");
            break;
        case 2:
            vertices = &AobjectVertices[0];
            normals = &AobjectNormals[0];
            indices = &AobjectIndices[0];
            texCoords = &AobjectTexCoords[0];
            numIndices = NUM_A_OBJECT_INDEX;
            LOG("----- IN JNI ----- detected marker ID 2");
            break;
        default:
            vertices = &RobjectVertices[0];
            normals = &RobjectNormals[0];
            indices = &RobjectIndices[0];
            texCoords = &RobjectTexCoords[0];
            numIndices = NUM_R_OBJECT_INDEX;
            LOG("----- IN JNI ----- detected marker ID 3");
            break;
        }

        QCAR::Matrix44F modelViewProjection;

        SampleUtils::translatePoseMatrix(-kLetterTranslate,
                                         -kLetterTranslate,
                                         0.f,
                                         &modelViewMatrix.data[0]);
        SampleUtils::scalePoseMatrix(kLetterScale, kLetterScale, kLetterScale,
                                     &modelViewMatrix.data[0]);
        SampleUtils::multiplyMatrix(&projectionMatrix.data[0],
                                    &modelViewMatrix.data[0],
                                    &modelViewProjection.data[0]);

        glUseProgram(shaderProgramID);
 
        glVertexAttribPointer(vertexHandle, 3, GL_FLOAT, GL_FALSE, 0, vertices);
        glVertexAttribPointer(normalHandle, 3, GL_FLOAT, GL_FALSE, 0, normals);
        glVertexAttribPointer(textureCoordHandle, 2, GL_FLOAT, GL_FALSE,
                              0, texCoords);

        glEnableVertexAttribArray(vertexHandle);
        glEnableVertexAttribArray(normalHandle);
        glEnableVertexAttribArray(textureCoordHandle);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, thisTexture->mTextureID);
        glUniformMatrix4fv(mvpMatrixHandle, 1, GL_FALSE,
                           (GLfloat*)&modelViewProjection.data[0]);
        glDrawElements(GL_TRIANGLES, numIndices, GL_UNSIGNED_SHORT, indices);

        SampleUtils::checkGlError("FrameMarkers render frame");

    }

    glDisable(GL_DEPTH_TEST);
    glDisableVertexAttribArray(vertexHandle);
    glDisableVertexAttribArray(normalHandle);
    glDisableVertexAttribArray(textureCoordHandle);

    QCAR::Renderer::getInstance().end();

    return markerId;
}


void
configureVideoBackground()
{
    // Get the default video mode:
    QCAR::CameraDevice& cameraDevice = QCAR::CameraDevice::getInstance();
    QCAR::VideoMode videoMode = cameraDevice.
                                getVideoMode(QCAR::CameraDevice::MODE_DEFAULT);

    // Configure the video background
    QCAR::VideoBackgroundConfig config;
    config.mEnabled = true;
    config.mSynchronous = true;
    config.mPosition.data[0] = 0.0f;
    config.mPosition.data[1] = 0.0f;
    
    if (isActivityInPortraitMode)
    {
        //LOG("configureVideoBackground PORTRAIT");
        config.mSize.data[0] = videoMode.mHeight
                                * (screenHeight / (float)videoMode.mWidth);
        config.mSize.data[1] = screenHeight;

        if(config.mSize.data[0] < screenWidth)
        {
            LOG("Correcting rendering background size to handle missmatch between screen and video aspect ratios.");
            config.mSize.data[0] = screenWidth;
            config.mSize.data[1] = screenWidth * 
                              (videoMode.mWidth / (float)videoMode.mHeight);
        }
    }
    else
    {
        //LOG("configureVideoBackground LANDSCAPE");
        config.mSize.data[0] = screenWidth;
        config.mSize.data[1] = videoMode.mHeight
                            * (screenWidth / (float)videoMode.mWidth);

        if(config.mSize.data[1] < screenHeight)
        {
            LOG("Correcting rendering background size to handle missmatch between screen and video aspect ratios.");
            config.mSize.data[0] = screenHeight
                                * (videoMode.mWidth / (float)videoMode.mHeight);
            config.mSize.data[1] = screenHeight;
        }
    }

    LOG("Configure Video Background : Video (%d,%d), Screen (%d,%d), mSize (%d,%d)", videoMode.mWidth, videoMode.mHeight, screenWidth, screenHeight, config.mSize.data[0], config.mSize.data[1]);

    // Set the config:
    QCAR::Renderer::getInstance().setVideoBackgroundConfig(config);
}


JNIEXPORT void JNICALL
Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkers_initApplicationNative(
                            JNIEnv* env, jobject obj, jint width, jint height)
{
    LOG("Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkers_initApplicationNative");
    
    // Store screen dimensions
    screenWidth = width;
    screenHeight = height;
        
    // Handle to the activity class:
    jclass activityClass = env->GetObjectClass(obj);

    jmethodID getTextureCountMethodID = env->GetMethodID(activityClass,
                                                    "getTextureCount", "()I");
    if (getTextureCountMethodID == 0)
    {
        LOG("Function getTextureCount() not found.");
        return;
    }

    textureCount = env->CallIntMethod(obj, getTextureCountMethodID);    
    if (!textureCount)
    {
        LOG("getTextureCount() returned zero.");
        return;
    }

    textures = new Texture*[textureCount];

    jmethodID getTextureMethodID = env->GetMethodID(activityClass,
        "getTexture", "(I)Lcom/qualcomm/QCARSamples/FrameMarkers/Texture;");

    if (getTextureMethodID == 0)
    {
        LOG("Function getTexture() not found.");
        return;
    }

    // Register the textures
    for (int i = 0; i < textureCount; ++i)
    {

        jobject textureObject = env->CallObjectMethod(obj, getTextureMethodID, i); 
        if (textureObject == NULL)
        {
            LOG("GetTexture() returned zero pointer");
            return;
        }

        textures[i] = Texture::create(env, textureObject);
    }
}


JNIEXPORT void JNICALL
Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkers_deinitApplicationNative(
                                                        JNIEnv* env, jobject obj)
{
    LOG("Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkers_deinitApplicationNative");

    // Release texture resources
    if (textures != 0)
    {    
        for (int i = 0; i < textureCount; ++i)
        {
            delete textures[i];
            textures[i] = NULL;
        }
    
        delete[]textures;
        textures = NULL;
        
        textureCount = 0;
    }
}


JNIEXPORT void JNICALL
Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkers_startCamera(JNIEnv *,
                                                                         jobject)
{
    LOG("Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkers_startCamera");

    // Initialize the camera:
    if (!QCAR::CameraDevice::getInstance().init())
        return;

    // Configure the video background
    configureVideoBackground();

    // Select the default mode:
    if (!QCAR::CameraDevice::getInstance().selectVideoMode(
                                QCAR::CameraDevice::MODE_DEFAULT))
        return;

    // Start the camera:
    if (!QCAR::CameraDevice::getInstance().start())
        return;

    // Start the tracker:
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::Tracker* markerTracker = trackerManager.getTracker(QCAR::Tracker::MARKER_TRACKER);
    if(markerTracker != 0)
        markerTracker->start();
}


JNIEXPORT void JNICALL
Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkers_stopCamera(JNIEnv *,
                                                                   jobject)
{
    LOG("Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkers_stopCamera");
    
    // Stop the tracker:
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::Tracker* markerTracker = trackerManager.getTracker(QCAR::Tracker::MARKER_TRACKER);
    if(markerTracker != 0)
        markerTracker->stop();
    
    QCAR::CameraDevice::getInstance().stop();
    QCAR::CameraDevice::getInstance().deinit();
}

JNIEXPORT void JNICALL
Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkers_setProjectionMatrix(JNIEnv *, jobject)
{
    LOG("Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkers_setProjectionMatrix");

    // Cache the projection matrix:
    const QCAR::CameraCalibration& cameraCalibration =
                                QCAR::CameraDevice::getInstance().getCameraCalibration();
    projectionMatrix = QCAR::Tool::getProjectionGL(cameraCalibration, 2.0f,
                                            2000.0f);
}

JNIEXPORT jboolean JNICALL
Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkers_activateFlash(JNIEnv*, jobject, jboolean flash)
{
    return QCAR::CameraDevice::getInstance().setFlashTorchMode((flash==JNI_TRUE)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkers_autofocus(JNIEnv*, jobject)
{
    return QCAR::CameraDevice::getInstance().setFocusMode(QCAR::CameraDevice::FOCUS_MODE_TRIGGERAUTO) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkers_setFocusMode(JNIEnv*, jobject, jint mode)
{
    int qcarFocusMode;

    switch ((int)mode)
    {
        case 0:
            qcarFocusMode = QCAR::CameraDevice::FOCUS_MODE_NORMAL;
            break;
        
        case 1:
            qcarFocusMode = QCAR::CameraDevice::FOCUS_MODE_CONTINUOUSAUTO;
            break;
            
        case 2:
            qcarFocusMode = QCAR::CameraDevice::FOCUS_MODE_INFINITY;
            break;
            
        case 3:
            qcarFocusMode = QCAR::CameraDevice::FOCUS_MODE_MACRO;
            break;
    
        default:
            return JNI_FALSE;
    }
    
    return QCAR::CameraDevice::getInstance().setFocusMode(qcarFocusMode) ? JNI_TRUE : JNI_FALSE;
}


JNIEXPORT void JNICALL
Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkersRenderer_initRendering(
                                                    JNIEnv* env, jobject obj)
{
    LOG("Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkersRenderer_initRendering");

    // Define clear color
    glClearColor(0.0f, 0.0f, 0.0f, QCAR::requiresAlpha() ? 0.0f : 1.0f);
    
    // Now generate the OpenGL texture objects and add settings
    for (int i = 0; i < textureCount; ++i)
    {
        glGenTextures(1, &(textures[i]->mTextureID));
        glBindTexture(GL_TEXTURE_2D, textures[i]->mTextureID);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, textures[i]->mWidth,
                textures[i]->mHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE,
                (GLvoid*)  textures[i]->mData);
    }
  
    shaderProgramID     = SampleUtils::createProgramFromBuffer(cubeMeshVertexShader,
                                                            cubeFragmentShader);

    vertexHandle        = glGetAttribLocation(shaderProgramID,
                                                "vertexPosition");
    normalHandle        = glGetAttribLocation(shaderProgramID,
                                                "vertexNormal");
    textureCoordHandle  = glGetAttribLocation(shaderProgramID,
                                                "vertexTexCoord");
    mvpMatrixHandle     = glGetUniformLocation(shaderProgramID,
                                                "modelViewProjectionMatrix");

}


JNIEXPORT void JNICALL
Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkersRenderer_updateRendering(
                        JNIEnv* env, jobject obj, jint width, jint height)
{
    LOG("Java_com_qualcomm_QCARSamples_FrameMarkers_FrameMarkersRenderer_updateRendering");
    
    // Update screen dimensions
    screenWidth = width;
    screenHeight = height;

    // Reconfigure the video background
    configureVideoBackground();
}


#ifdef __cplusplus
}
#endif
