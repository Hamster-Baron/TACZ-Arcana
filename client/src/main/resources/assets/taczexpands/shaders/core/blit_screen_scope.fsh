#version 150

uniform sampler2D DiffuseSampler;

uniform vec4 ColorModulator;
uniform int ThermalImaging;
uniform int Monochrome;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    if (ThermalImaging == 1 && color.r > 0.999) {
        if (Monochrome == 1) {
            color.rgb *= 1.5;
        }

        color.r = max(color.r, 0.8);
    } else {
        color *= vertexColor;
    }

    fragColor = color * ColorModulator;

    if (Monochrome == 1) {
        float luminance = dot(fragColor.rgb, vec3(0.299, 0.587, 0.114));
        fragColor = vec4(vec3(luminance), fragColor.a);
    }
}
