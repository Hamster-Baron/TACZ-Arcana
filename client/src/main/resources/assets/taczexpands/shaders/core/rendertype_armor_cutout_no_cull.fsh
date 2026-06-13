#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec2 texCoord1;
in vec4 normal;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }
    float gray = dot(color.rgb, vec3(0.3, 0.59, 0.11));
    fragColor = mix(vec4(gray, gray, gray, color.a), vec4(1.0, 0.2, 0.0, 1.0), 0.7);
    fragColor.r = 1.0;
}
