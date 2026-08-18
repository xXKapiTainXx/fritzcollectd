(function () {
  try {
    var d = document;
    var head = d.head || d.documentElement;

    var viewport = d.querySelector('meta[name="viewport"]');
    if (!viewport) {
      viewport = d.createElement('meta');
      viewport.setAttribute('name', 'viewport');
      head.appendChild(viewport);
    }
    viewport.setAttribute('content', 'width=device-width,initial-scale=1,maximum-scale=5,user-scalable=yes');

    var css = [
      'html,body{max-width:100%!important;overflow-x:hidden!important;-webkit-text-size-adjust:100%!important}',
      'body{font-size:16px!important;box-sizing:border-box!important}',
      '*,*:before,*:after{box-sizing:border-box!important}',
      'main,.main-content,#main,.content,#content,.page-content,.content-wrapper,.wrapper{margin-left:0!important;max-width:100%!important;width:100%!important}',
      '.container,.container-fluid{max-width:100%!important;width:100%!important;padding-left:12px!important;padding-right:12px!important}',
      'img,svg,video,canvas{max-width:100%!important;height:auto}',
      'input:not([type=checkbox]):not([type=radio]),select,textarea{width:100%!important;max-width:100%!important;min-height:44px!important;font-size:16px!important}',
      'button,.btn,input[type=button],input[type=submit],a.btn{min-height:44px!important;font-size:15px!important;touch-action:manipulation}',
      'input[type=checkbox],input[type=radio]{min-width:20px!important;min-height:20px!important}',
      'table{display:block!important;width:100%!important;max-width:100%!important;overflow-x:auto!important;-webkit-overflow-scrolling:touch!important;border-collapse:collapse}',
      'thead,tbody,tfoot{width:max-content;min-width:100%}',
      'th,td{white-space:nowrap!important;vertical-align:middle!important}',
      '.table-responsive{width:100%!important;max-width:100%!important;overflow-x:auto!important;-webkit-overflow-scrolling:touch!important}',
      '.modal-dialog{margin:10px!important;width:auto!important;max-width:calc(100% - 20px)!important}',
      '.modal-content{max-width:100%!important}',
      '.dropdown-menu{max-width:calc(100vw - 24px)!important;overflow-x:auto!important}',
      'pre,code{white-space:pre-wrap!important;word-break:break-word!important}',
      '.__3dp_hidden_nav{display:none!important}',
      '#__3dp_mobile_overlay{display:none;position:fixed!important;z-index:2147483646!important;inset:0!important;background:rgba(0,0,0,.58)!important;margin:0!important;padding:0!important}',
      '#__3dp_mobile_overlay.__3dp_open{display:block!important}',
      '#__3dp_mobile_panel{position:absolute!important;left:0!important;top:0!important;bottom:0!important;width:min(86vw,360px)!important;overflow-y:auto!important;background:#111923!important;color:#fff!important;padding:14px!important;box-shadow:8px 0 28px rgba(0,0,0,.35)!important}',
      '#__3dp_mobile_head{display:flex!important;align-items:center!important;justify-content:space-between!important;font:700 21px Arial,sans-serif!important;padding:8px 4px 14px!important;border-bottom:1px solid #2d3a49!important}',
      '#__3dp_mobile_close{border:0!important;background:transparent!important;color:#fff!important;font-size:30px!important;min-height:40px!important;padding:0 8px!important}',
      '#__3dp_mobile_links{padding:10px 0 30px!important}',
      '#__3dp_mobile_links a{display:block!important;color:#fff!important;text-decoration:none!important;font:500 16px Arial,sans-serif!important;padding:13px 12px!important;margin:3px 0!important;border-radius:10px!important;background:#182331!important;white-space:normal!important}',
      '#__3dp_mobile_links a:active{background:#26384d!important}',
      '#__3dp_mobile_empty{padding:18px 10px!important;color:#c9d3dd!important;font:15px Arial,sans-serif!important;line-height:1.45!important}',
      '@media(max-width:780px){.row{margin-left:-6px!important;margin-right:-6px!important}.row>[class*=col-md-],.row>[class*=col-lg-],.row>[class*=col-xl-]{flex:0 0 100%!important;max-width:100%!important;width:100%!important}.card,.panel{max-width:100%!important;margin-left:0!important;margin-right:0!important}.btn-group{max-width:100%!important;flex-wrap:wrap!important}.navbar{max-width:100%!important;overflow-x:auto!important}}'
    ].join('');

    var style = d.getElementById('__3dp_mobile_style');
    if (!style) {
      style = d.createElement('style');
      style.id = '__3dp_mobile_style';
      head.appendChild(style);
    }
    style.textContent = css;

    var oldHidden = d.querySelectorAll('.__3dp_hidden_nav');
    for (var oh = 0; oh < oldHidden.length; oh++) {
      oldHidden[oh].classList.remove('__3dp_hidden_nav');
    }

    var sideSelectors = [
      'aside', '#sidebar', '.sidebar', '.sidenav', '.side-nav',
      '[class*="sidebar"]', '[id*="sidebar"]'
    ];
    var sidebar = null;
    for (var si = 0; si < sideSelectors.length && !sidebar; si++) {
      var candidates = d.querySelectorAll(sideSelectors[si]);
      for (var ci = 0; ci < candidates.length; ci++) {
        if (candidates[ci].querySelectorAll('a[href]').length >= 2) {
          sidebar = candidates[ci];
          break;
        }
      }
    }

    var roots = [];
    if (sidebar) {
      roots.push(sidebar);
    } else {
      var navs = d.querySelectorAll('nav,[role="navigation"],header .nav,header ul');
      for (var ni = 0; ni < navs.length; ni++) {
        if (navs[ni].querySelectorAll('a[href]').length >= 2) roots.push(navs[ni]);
      }
    }

    var links = [];
    var seen = {};
    function collect(root) {
      if (!root) return;
      var anchors = root.querySelectorAll('a[href]');
      for (var i = 0; i < anchors.length; i++) {
        var a = anchors[i];
        var text = (a.textContent || '').replace(/\s+/g, ' ').trim();
        var href = a.getAttribute('href') || '';
        if (!text || !href || href === '#' || /^javascript:/i.test(href)) continue;
        var absolute = a.href || href;
        var key = text + '|' + absolute;
        if (seen[key]) continue;
        seen[key] = true;
        links.push({ text: text, href: absolute });
      }
    }
    for (var ri = 0; ri < roots.length; ri++) collect(roots[ri]);

    if (sidebar && links.length >= 2) sidebar.classList.add('__3dp_hidden_nav');

    var overlay = d.getElementById('__3dp_mobile_overlay');
    if (overlay && overlay.parentNode) overlay.parentNode.removeChild(overlay);

    overlay = d.createElement('div');
    overlay.id = '__3dp_mobile_overlay';
    overlay.onclick = function (e) {
      if (e.target === overlay) overlay.classList.remove('__3dp_open');
    };

    var panel = d.createElement('div');
    panel.id = '__3dp_mobile_panel';
    overlay.appendChild(panel);

    var h = d.createElement('div');
    h.id = '__3dp_mobile_head';
    var ht = d.createElement('span');
    ht.textContent = '3D Pilot';
    h.appendChild(ht);
    var close = d.createElement('button');
    close.id = '__3dp_mobile_close';
    close.type = 'button';
    close.textContent = '×';
    close.onclick = function () { overlay.classList.remove('__3dp_open'); };
    h.appendChild(close);
    panel.appendChild(h);

    var list = d.createElement('div');
    list.id = '__3dp_mobile_links';
    panel.appendChild(list);

    if (links.length) {
      for (var li = 0; li < links.length; li++) {
        var item = d.createElement('a');
        item.href = links[li].href;
        item.textContent = links[li].text;
        item.onclick = function () { overlay.classList.remove('__3dp_open'); };
        list.appendChild(item);
      }
    } else {
      var empty = d.createElement('div');
      empty.id = '__3dp_mobile_empty';
      empty.textContent = 'Die vollständige Navigation wird nach der Anmeldung automatisch hier angezeigt.';
      list.appendChild(empty);
    }

    (d.body || d.documentElement).appendChild(overlay);
    window.__threeDPilotToggleMenu = function () {
      var o = d.getElementById('__3dp_mobile_overlay');
      if (o) o.classList.toggle('__3dp_open');
    };
  } catch (e) {
  }
})();
