// mc-status.js
class MinecraftStatus {
  constructor(server = "mc.mcme.uno", containerId = "mc-status") {
    this.server = server;
    this.containerId = containerId;
    this.init();
  }
  
  async init() {
    this.createUI();
    await this.update();
  }
  
  createUI() {
    const container = document.getElementById(this.containerId);
    container.innerHTML = `
      <div style="border:1px solid #ddd; padding:10px; border-radius:5px;">
        <h4 style="margin:0 0 10px 0;">${this.server} 服务器</h4>
        <p id="mc-count">加载中...</p>
        <p id="mc-list" style="font-size:14px;"></p>
        <button onclick="mcStatus.update()" style="font-size:12px;">刷新</button>
      </div>
    `;
  }
  
  async update() {
    const countEl = document.getElementById("mc-count");
    const listEl = document.getElementById("mc-list");
    
    try {
      const response = await fetch(`https://api.mcsrvstat.us/2/${this.server}`);
      const data = await response.json();
      
      if (data.online && data.players) {
        countEl.textContent = `在线: ${data.players.online}/${data.players.max}`;
        listEl.textContent = data.players.list ? data.players.list.join(", ") : "(无在线玩家)";
      } else {
        countEl.textContent = "服务器离线";
        listEl.textContent = "";
      }
    } catch(e) {
      countEl.textContent = "查询失败";
      listEl.textContent = "";
    }
  }
}

// 全局变量便于使用
window.mcStatus = new MinecraftStatus();
