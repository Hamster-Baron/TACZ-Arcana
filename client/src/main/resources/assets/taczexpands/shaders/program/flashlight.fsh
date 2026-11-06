#version 150
uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D FlashlightDepthSampler;

uniform mat4 ProjMat;
uniform vec2 OutSize;
uniform mat4 InvProjMat;
uniform mat4 InvModelViewMat;
uniform mat4 MainCameraToWorldMat;
uniform mat4 FlashlightProjMat;
uniform mat4 FlashlightModelViewMat;
uniform vec3 WorldOffset;
uniform vec2 ShadowPixelStep;
uniform vec3 From;
uniform vec3 To;
uniform float Angle;
uniform float Range;
uniform float Luminance;
uniform float MinZ;

in vec2 texCoord;
out vec4 fragColor;

float standardBrightness = 0.3;
float aberrationStrength = 0.05;
const float MaxAmbientLight = 0.2;

vec3 getFragWorldPos(vec2 coord, float depth) {
    vec4 clipPos = vec4(coord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 viewPos = InvProjMat * clipPos;
    viewPos /= viewPos.w;
    return vec3(InvModelViewMat * viewPos);
}

vec3 getLightProjCoord(vec3 cameraPos) {
    vec4 worldPos = MainCameraToWorldMat * vec4(cameraPos, 1.0);
    worldPos.xyz += WorldOffset;
    vec4 modelPos = FlashlightModelViewMat * worldPos;
    vec4 projPos = FlashlightProjMat * modelPos;
    vec3 projCoords = projPos.xyz / projPos.w;
    vec2 lightUV = projCoords.xy * 0.5 + 0.5;
    float currentDepth = projCoords.z * 0.5 + 0.5;
    return vec3(lightUV.x, lightUV.y, currentDepth);
}

float linearDepth(float depth) {
    return FlashlightProjMat[3][2] / (2.0 * depth - 1.0 + FlashlightProjMat[2][2]);
}

// Choose neighbors on the receiver's plane instead of differentiating across a corner.
vec3 receiverEdge(vec2 step, float depth, vec3 coord, out bool discontinuity) {
    float forward = texture(DiffuseDepthSampler, texCoord + step).r;
    float backward = texture(DiffuseDepthSampler, texCoord - step).r;
    float forward2 = texture(DiffuseDepthSampler, texCoord + 2.0 * step).r;
    float backward2 = texture(DiffuseDepthSampler, texCoord - 2.0 * step).r;
    float forwardError = abs(2.0 * forward - forward2 - depth);
    float backwardError = abs(2.0 * backward - backward2 - depth);
    discontinuity = max(forwardError, backwardError) > 8.0 * max(min(forwardError, backwardError), 1e-7);
    bool useForward = forwardError < backwardError;
    vec2 neighbor = texCoord + (useForward ? step : -step);
    vec3 edge = getLightProjCoord(getFragWorldPos(neighbor, useForward ? forward : backward)) - coord;
    return useForward ? edge : -edge;
}

vec2 receiverGradient(vec3 dx, vec3 dy) {
    float determinant = dx.x * dy.y - dx.y * dy.x;
    return abs(determinant) > 1e-12
        ? vec2(dy.y * dx.z - dx.y * dy.z, dx.x * dy.z - dy.x * dx.z) / determinant
        : vec2(0.0);
}

float shadowPCF(sampler2D depthSampler, vec2 uv, float currentDepth, vec2 gradient, float receiverFootprint, float baseBias, bool discontinuity) {
    float shadow = 0.0;
    const int KernelSize = 9;
    float scaledBias = baseBias;
    vec2 stepSize = ShadowPixelStep;

    // Filter a single receiver plane; geometric edges use the center sample.
    int halfKernel = discontinuity ? 0 : KernelSize / 2;
    float samples = float(2 * halfKernel + 1);
    for (int x = -halfKernel; x <= halfKernel; x++) {
        for (int y = -halfKernel; y <= halfKernel; y++) {

            vec2 offset = vec2(float(x), float(y)) * stepSize;

            vec2 sampleUV = (floor((uv + offset) / ShadowPixelStep) + 0.5) * ShadowPixelStep;
            sampleUV = clamp(sampleUV, ShadowPixelStep * 0.5, vec2(1.0) - ShadowPixelStep * 0.5);
            float receiver = linearDepth(currentDepth + dot(gradient, sampleUV - uv) - receiverFootprint);
            float visibility = (receiver <= linearDepth(texture(depthSampler, sampleUV).r) + scaledBias) ? 1.0 : 0.0;
            shadow += visibility;
        }
    }

    return shadow / (samples * samples);
}

void main() {
    vec4 diffuseColor = texture(DiffuseSampler, texCoord);
    float depth = texture(DiffuseDepthSampler, texCoord).r;
    vec3 pos = getFragWorldPos(texCoord, depth);
    vec3 lightProjCoord = getLightProjCoord(pos);

    vec2 lightUV = lightProjCoord.xy;
    float lightDepth = lightProjCoord.z;

    vec3 finalColor = diffuseColor.rgb;
    float visibility = 0.0;

    vec3 lightDir = pos - From;
    float dist = length(lightDir);

    if (dist > Range || depth < MinZ) {
        fragColor = vec4(diffuseColor.rgb, 1.0);
        return;
    }

    lightDir = normalize(lightDir);
    vec3 lightTo = normalize(To - From);
    float angleCos = dot(lightDir, lightTo);
    float cutoffCos = cos(Angle);
    if (angleCos <= cutoffCos) {
        fragColor = vec4(diffuseColor.rgb, 1.0);
        return;
    }

    if (lightUV.x >= 0.0 && lightUV.x <= 1.0 &&
    lightUV.y >= 0.0 && lightUV.y <= 1.0 &&
    lightDepth >= 0.0 && lightDepth <= 1.0) {
        bool edgeX, edgeY;
        vec3 receiverX = receiverEdge(vec2(1.0 / OutSize.x, 0.0), depth, lightProjCoord, edgeX);
        vec3 receiverY = receiverEdge(vec2(0.0, 1.0 / OutSize.y), depth, lightProjCoord, edgeY);
        vec2 gradient = receiverGradient(receiverX, receiverY);
        // Bound half-pixel camera jitter against the receiver plane at the sampled depth.
        vec3 jitterX = getLightProjCoord(getFragWorldPos(texCoord + vec2(0.5 / OutSize.x, 0.0), depth)) - lightProjCoord;
        vec3 jitterY = getLightProjCoord(getFragWorldPos(texCoord + vec2(0.0, 0.5 / OutSize.y), depth)) - lightProjCoord;
        float receiverFootprint = abs(jitterX.z - dot(gradient, jitterX.xy))
            + abs(jitterY.z - dot(gradient, jitterY.xy));
        float bias = 0.02;
        visibility = shadowPCF(FlashlightDepthSampler, lightUV, lightDepth, gradient, receiverFootprint, bias, edgeX || edgeY);
    } else {
        visibility = 1.0;
    }


    float intensity = 0.0;

    if (angleCos > cutoffCos) {
        float spotIntensity = smoothstep(cutoffCos, 1.0, angleCos);

        intensity = spotIntensity * (1.0 - pow(dist / Range, 3.0)) * clamp(Luminance, 0.0, 2.75);
        intensity = mix(0.0, intensity * 1.6, 1.0 - pow(dist / Range, 2.0));
    } else {
        fragColor = vec4(diffuseColor.rgb, 1.0);
        return;
    }

    float brightness = dot(diffuseColor.rgb, vec3(0.299, 0.587, 0.114));
    float adjustedIntensity;

    if (brightness <= standardBrightness) {
        float factor = 1.0 - (brightness / standardBrightness);
        adjustedIntensity = intensity * (1.0 + pow(factor, 3.5) * 2.0);
    } else {
        float factor = 1.0 - (brightness - standardBrightness) / (1.0 - standardBrightness);
        adjustedIntensity = intensity * max(factor, 0.0);
    }

    vec2 center = vec2(0.5, 0.5);
    vec2 offsetDir = lightUV - center;
    vec3 colorOffset = vec3(1.0);
    colorOffset.r -= offsetDir.x * aberrationStrength;
    colorOffset.g -= offsetDir.y * aberrationStrength * 0.5;
    colorOffset.b += offsetDir.x * aberrationStrength;
    colorOffset = clamp(colorOffset, 0.8, 1.2);

    visibility = smoothstep(0.1, 0.9, visibility);

    float shadowFadeFactor = 1.0 - pow(dist / Range, 3.0);
    shadowFadeFactor = smoothstep(0.0, 0.2, shadowFadeFactor);

    float ambientContribution = MaxAmbientLight * shadowFadeFactor;
    visibility = mix(ambientContribution, 1.0, visibility);


    vec3 lightContribution = adjustedIntensity * colorOffset;

    vec3 lightAdd = lightContribution * visibility;

    vec3 finalLitColor = diffuseColor.rgb * (vec3(1.0) + lightAdd);
//    if (finalLitColor.r > 0.999) {
//        finalLitColor.r = 0.90;
//    }
    fragColor = vec4(finalLitColor.rgb, 1.0);
}
