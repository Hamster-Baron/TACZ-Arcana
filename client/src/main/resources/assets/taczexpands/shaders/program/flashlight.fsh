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

vec3 getFragWorldPos(vec2 coord) {
    float depth = texture(DiffuseDepthSampler, coord).r;
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

float shadowPCF(sampler2D depthSampler, vec2 uv, float currentDepth, float baseBias) {
    float shadow = 0.0;
    const int KernelSize = 9;
    const float TotalSamples = float(KernelSize * KernelSize);
    float scaledBias = baseBias;
    const float baseMultiplier = 4.0;
    vec2 stepSize = ShadowPixelStep * baseMultiplier;

    int halfKernel = KernelSize / 2;
    for (int x = -halfKernel; x <= halfKernel; x++) {
        for (int y = -halfKernel; y <= halfKernel; y++) {

            vec2 offset = vec2(float(x), float(y)) * stepSize;

            float visibility = (currentDepth <= texture(depthSampler, uv + offset).r + scaledBias) ? 1.0 : 0.0;
            shadow += visibility;
        }
    }

    return shadow / TotalSamples;
}

void main() {
    vec4 diffuseColor = texture(DiffuseSampler, texCoord);
    float depth = texture(DiffuseDepthSampler, texCoord).r;
    vec3 pos = getFragWorldPos(texCoord);
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

    if (lightUV.x >= 0.0 && lightUV.x <= 1.0 &&
    lightUV.y >= 0.0 && lightUV.y <= 1.0 &&
    lightDepth >= 0.0 && lightDepth <= 1.0) {
        float bias = 0.001;
        visibility = shadowPCF(FlashlightDepthSampler, lightUV, lightDepth, bias);
    } else {
        visibility = 1.0;
    }

    
    lightDir = normalize(lightDir);
    vec3 lightTo = normalize(To - From);
    float angleCos = dot(lightDir, lightTo);
    float intensity = 0.0;
    float cutoffCos = cos(Angle);

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
    fragColor = vec4(finalLitColor.rgb, 1.0);
}