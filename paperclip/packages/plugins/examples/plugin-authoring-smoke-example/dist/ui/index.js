// src/ui/index.tsx
import { usePluginAction, usePluginData } from "@paperclipai/plugin-sdk/ui";
import { jsx, jsxs } from "react/jsx-runtime";
function DashboardWidget(_props) {
  const { data, loading, error } = usePluginData("health");
  const ping = usePluginAction("ping");
  if (loading) return /* @__PURE__ */ jsx("div", { children: "Loading plugin health..." });
  if (error) return /* @__PURE__ */ jsxs("div", { children: [
    "Plugin error: ",
    error.message
  ] });
  return /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: "0.5rem" }, children: [
    /* @__PURE__ */ jsx("strong", { children: "Plugin Authoring Smoke Example" }),
    /* @__PURE__ */ jsxs("div", { children: [
      "Health: ",
      data?.status ?? "unknown"
    ] }),
    /* @__PURE__ */ jsxs("div", { children: [
      "Checked: ",
      data?.checkedAt ?? "never"
    ] }),
    /* @__PURE__ */ jsx("button", { onClick: () => void ping(), children: "Ping Worker" })
  ] });
}
export {
  DashboardWidget
};
//# sourceMappingURL=index.js.map
