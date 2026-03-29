import {
  VISUAL_EDITOR_DOM_FLAG,
  VISUAL_EDITOR_MSG_COMMAND,
  VISUAL_EDITOR_MSG_ELEMENT_SELECTED,
} from '@/utils/visualEditorConstants'

/**
 * 生成注入到 iframe 的引导脚本（IIFE，不挂载 window 属性；状态在闭包内）。
 * 通过 postMessage 接收父页面的 visual-editor-command 启停，避免重复注入与竞态。
 *
 * tagPath 沿祖先尽量采满整链；脚本内 MAX=512 仅防止异常 DOM 死循环，与前端展示层数无关。
 */
export function createVisualEditorBootstrapScript(parentOrigin: string): string {
  const PO = JSON.stringify(parentOrigin)
  const cmd = JSON.stringify(VISUAL_EDITOR_MSG_COMMAND)
  const sel = JSON.stringify(VISUAL_EDITOR_MSG_ELEMENT_SELECTED)
  const flag = JSON.stringify(VISUAL_EDITOR_DOM_FLAG)

  return (
    '(function(PARENT_ORIGIN){' +
    '"use strict";' +
    'var CMD=' +
    cmd +
    ';' +
    'var SEL=' +
    sel +
    ';' +
    'var FLAG=' +
    flag +
    ';' +
    'var HOVER_STYLE="2px dashed #1677ff";' +
    'var SELECT_STYLE="2px solid #1677ff";' +
    'var active=false;' +
    'var hovered=null;' +
    'var selected=null;' +
    'function safeOutline(el,v){try{if(el){el.style.outline=v||"";el.style.outlineOffset=v?"0px":"";}}catch(e){}}' +
    'function clearHover(){try{if(hovered&&hovered!==selected)safeOutline(hovered,"");}catch(e){}hovered=null;}' +
    'function applyHover(el){try{clearHover();if(!el||el===selected)return;if(el===document.documentElement||el===document.body)return;hovered=el;safeOutline(hovered,HOVER_STYLE);}catch(e){}}' +
    'function applySelect(el){try{if(selected&&selected!==el){safeOutline(selected,"");}selected=el;if(selected)safeOutline(selected,SELECT_STYLE);}catch(e){}}' +
    'function xpathFor(el){try{if(!el||el.nodeType!==1)return"";if(el.id)return\'//*[@id="\'+String(el.id).replace(/"/g,\'\\\\"\')+\'"]\';var parts=[];var cur=el;for(var i=0;i<16&&cur&&cur.nodeType===1&&cur!==document.body;i++){var tag=cur.tagName?cur.tagName.toLowerCase():"*";var ix=1;var sib=cur;while((sib=sib.previousElementSibling)){if(sib.tagName===cur.tagName)ix++;}parts.unshift(tag+"["+ix+"]");cur=cur.parentElement;}return"/"+parts.join("/");}catch(e){return""}}' +
    'function tagPathFor(el){var a=[];var c=el;var n=0;var MAX=512;try{while(c&&c.nodeType===1&&n<MAX){a.unshift((c.tagName||"*").toLowerCase());c=c.parentElement;n++;}}catch(e){}return a.length?a:["unknown"];}' +
    'function payloadFor(el){try{var r=el.getBoundingClientRect();return{' +
    'tag:String(el.tagName||"").toLowerCase(),' +
    'tagPath:tagPathFor(el),' +
    'id:el.id||undefined,' +
    'classList:el.classList&&el.classList.length?[].slice.call(el.classList):undefined,' +
    'text:(String(el.innerText||"").trim().slice(0,300))||undefined,' +
    'xpath:xpathFor(el)||undefined,' +
    'bounding:{x:r.x,y:r.y,width:r.width,height:r.height,top:r.top,left:r.left,right:r.right,bottom:r.bottom}' +
    '};}catch(e){return{tag:"unknown"}}}' +
    'function onMove(e){try{if(!active)return;var t=e.target;if(!t||t.nodeType!==1)return;if(t===document.documentElement||t===document.body){clearHover();return;}applyHover(t);}catch(err){}}' +
    'function onClick(e){try{if(!active)return;var t=e.target;if(!t||t.nodeType!==1)return;e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();applySelect(t);clearHover();parent.postMessage({type:SEL,payload:payloadFor(t)},PARENT_ORIGIN);}catch(err){}}' +
    'function onDocLeave(){try{if(!active)return;clearHover();}catch(err){}}' +
    'function attach(){if(active)return;active=true;document.addEventListener("mousemove",onMove,true);document.addEventListener("click",onClick,true);document.addEventListener("mouseleave",onDocLeave,true);}' +
    'function detach(){if(!active)return;active=false;document.removeEventListener("mousemove",onMove,true);document.removeEventListener("click",onClick,true);document.removeEventListener("mouseleave",onDocLeave,true);try{clearHover();if(selected)safeOutline(selected,"");}catch(e){}selected=null;hovered=null;}' +
    'function onMsg(ev){try{if(ev.source!==parent)return;if(ev.origin!==PARENT_ORIGIN)return;var d=ev.data;if(!d||d.type!==CMD)return;if(d.active)attach();else detach();}catch(e){}}' +
    'window.addEventListener("message",onMsg);' +
    'document.documentElement.setAttribute(FLAG,"1");' +
    '})(' +
    PO +
    ');'
  )
}
