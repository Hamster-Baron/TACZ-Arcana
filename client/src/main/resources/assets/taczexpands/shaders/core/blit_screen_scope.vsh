#version 150

in vec3 Position;
in vec2 UV;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int ThermalImaging;

out vec2 texCoord;
out vec4 vertexColor;

void main() {
    vec4 pos = ProjMat * ModelViewMat * vec4(Position, 1.0);

    gl_Position = vec4(pos.xy, pos.w, pos.w);
    texCoord = UV;
    vertexColor = Color;
}
