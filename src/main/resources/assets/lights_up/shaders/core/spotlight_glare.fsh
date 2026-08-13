#version 330

#define MAX_GLARES 4

layout(std140) uniform GlareParameters {
    vec4 GlareInfo;
    vec4 GlarePos[MAX_GLARES];
    vec4 GlareColor[MAX_GLARES];
};

in vec2 screenPos;
out vec4 fragColor;

void main() {
    float aspect = GlareInfo.x;
    float whiteout = GlareInfo.y;
    int count = int(GlareInfo.z + 0.5);

    vec2 p = vec2(screenPos.x * aspect, screenPos.y);

    vec3 accum = vec3(0.0);

    for (int i = 0; i < MAX_GLARES; i++) {
        if (i >= count) break;

        vec2 center = vec2(GlarePos[i].x * aspect, GlarePos[i].y);
        float strength = GlarePos[i].z;
        float radius = max(GlarePos[i].w, 1e-3);

        float d = length(p - center);
        float r2 = radius * radius;
        float d2 = d * d;

        float core = exp(-d2 / r2);
        float halo = r2 / (r2 + d2 * 6.0);

        accum += GlareColor[i].rgb * strength * (core * 1.5 + halo * 0.5);
    }

    accum += vec3(whiteout);

    fragColor = vec4(accum, 1.0);
}
