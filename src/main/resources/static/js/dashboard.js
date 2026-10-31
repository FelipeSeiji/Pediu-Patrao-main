(() => {
    'use strict';
    const $ = id => document.getElementById(id);
    const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
    const inteiro = new Intl.NumberFormat('pt-BR');
    const labels = {RECEBIDO:'Recebido',EM_PREPARACAO:'Em preparação',PRONTO:'Pronto',SAIU_PARA_ENTREGA:'Em entrega',RETIRADO:'Retirado',FINALIZADO:'Finalizado',CANCELADO:'Cancelado'};
    const colors = {RECEBIDO:'#91acd2',EM_PREPARACAO:'#edaa65',PRONTO:'#91bfc1',SAIU_PARA_ENTREGA:'#a695c8',RETIRADO:'#94b5a0',FINALIZADO:'#66a58d',CANCELADO:'#d88c83'};
    let dados, consulta, hoje, pagina = 0, totalPaginas = 0, metrica = 'faturamento';
    let controller, versao = 0, carregando = false, carregandoPagina = false;
    const dataLocal = date => new Date(`${date}T12:00:00Z`).toLocaleDateString('pt-BR', {timeZone:'America/Sao_Paulo'});
    const deslocar = (date, dias) => { const d = new Date(`${date}T12:00:00Z`); d.setUTCDate(d.getUTCDate()+dias); return d.toISOString().slice(0,10); };
    const node = (tag, text, className) => { const n = document.createElement(tag); if (text != null) n.textContent = text; if (className) n.className = className; return n; };
    function erro(message) { $('erro-texto').textContent = message; $('erro').hidden = false; }
    async function json(url, signal) {
        const timeout = new AbortController();
        const timer = setTimeout(() => timeout.abort(), 15000);
        try {
            const response = await fetch(url, {headers:{Accept:'application/json'}, credentials:'same-origin', cache:'no-store', signal:signal ? AbortSignal.any([signal,timeout.signal]) : timeout.signal});
            if (response.status === 401) throw new Error('Sua sessão expirou. Entre novamente no sistema para continuar.');
            if (response.status === 403) throw new Error('Apenas administrador e gerente podem consultar este painel.');
            if (!response.ok) { const problem = await response.json().catch(() => ({})); throw new Error(problem.detail || 'Não foi possível consultar os dados. Tente novamente.'); }
            return await response.json();
        } catch (e) {
            if (timeout.signal.aborted) throw new Error('A consulta demorou mais que o esperado. Tente novamente.');
            if (e instanceof TypeError) throw new Error('Sem conexão com a API. Verifique sua conexão e tente novamente.');
            throw e;
        } finally { clearTimeout(timer); }
    }
    function params() {
        return new URLSearchParams({inicio:$('inicio').value, fim:$('fim').value, status:$('status').value, agrupamento:$('agrupamento').value});
    }
    async function atualizar(novosFiltros = false) {
        if (!hoje) { await iniciar(); return; }
        if (novosFiltros && !$('filtros').reportValidity()) return;
        if (novosFiltros && ($('inicio').value > $('fim').value || (new Date($('fim').value)-new Date($('inicio').value))/86400000 >= 366)) {
            erro('Selecione um período de 1 a 366 dias, com início anterior ou igual ao fim.'); return;
        }
        if (novosFiltros || !consulta) { consulta = params(); pagina = 0; }
        controller?.abort(); controller = new AbortController();
        const minhaVersao = ++versao;
        carregando = true;
        $('resultados').setAttribute('aria-busy','true'); $('atualizar').disabled = true;
        $('atualizacao').textContent = 'Atualizando dados…'; $('erro').hidden = true;
        $('anterior').disabled = true; $('proxima').disabled = true;
        try {
            const [resumo, pedidos] = await Promise.all([
                json(`/api/dashboard?${consulta}`,controller.signal),
                json(`/api/dashboard/pedidos?${consulta}&pagina=${pagina}&tamanho=8`,controller.signal)
            ]);
            if (minhaVersao !== versao) return;
            dados = resumo; renderizar(resumo); renderizarPedidos(pedidos);
            $('resultados').hidden = false;
            const url = new URL(window.location.href); url.search = consulta.toString(); history.replaceState(null,'',url);
        } catch (e) {
            if (minhaVersao !== versao || controller.signal.aborted) return;
            erro(e.message);
            if (novosFiltros || !dados) { $('resultados').hidden = true; $('vazio').hidden = true; }
            $('atualizacao').textContent = 'Falha na atualização — dados anteriores podem estar desatualizados';
        } finally {
            if (minhaVersao === versao) { carregando = false; $('resultados').setAttribute('aria-busy','false'); $('atualizar').disabled = false; }
        }
    }
    function renderizar(d) {
        const i = d.indicadores;
        $('periodo').textContent = `${dataLocal(d.filtros.inicio)} a ${dataLocal(d.filtros.fim)} · ${labels[d.filtros.status] || 'Todos os pedidos'}`;
        $('atualizacao').textContent = `Atualizado às ${new Date(d.atualizadoEm).toLocaleTimeString('pt-BR',{timeZone:d.fusoHorario,hour:'2-digit',minute:'2-digit',second:'2-digit'})} · São Paulo`;
        $('faturamento').textContent = brl.format(i.faturamento); $('pedidos').textContent = inteiro.format(i.pedidos);
        $('ticket').textContent = brl.format(i.ticketMedio); $('cancelados').textContent = inteiro.format(i.cancelados);
        $('andamento').textContent = `${inteiro.format(i.emAndamento)} em andamento · ${inteiro.format(i.finalizados)} finalizados`;
        $('taxa').textContent = `${inteiro.format(i.taxaCancelamento)}% dos pedidos selecionados`;
        const v = d.comparacao.variacaoFaturamento;
        $('comparacao').textContent = v == null ? 'Sem base de faturamento no período anterior' : `${v >= 0 ? '↗ +' : '↘ '}${inteiro.format(v)}% vs. período anterior`;
        $('comparacao').className = v == null ? '' : v >= 0 ? 'positive' : 'negative';
        $('comparacao').title = `${dataLocal(d.comparacao.inicio)} a ${dataLocal(d.comparacao.fim)}: ${brl.format(d.comparacao.faturamento)}`;
        $('total-finalizados').textContent = `${inteiro.format(i.finalizados)} pedidos finalizados`;
        $('vazio').hidden = i.pedidos !== 0;
        desenharGrafico(); desenharSituacoes(d); ranking(d.produtos);
        const tabela = $('serie-tabela'); tabela.replaceChildren();
        d.vendas.forEach(venda => { const tr = node('tr'); [dataLocal(venda.data),inteiro.format(venda.pedidos),brl.format(venda.faturamento)].forEach(v => tr.append(node('td',v))); tabela.append(tr); });
    }
    function desenharGrafico() {
        if (!dados) return;
        const serie = dados.vendas;
        const ns = 'http://www.w3.org/2000/svg';
        const svg = (tag,attrs,text) => { const el = document.createElementNS(ns,tag); Object.entries(attrs).forEach(([k,v])=>el.setAttribute(k,String(v))); if (text != null) el.textContent=text; return el; };
        const root = svg('svg',{viewBox:'0 0 660 245',role:'img','aria-labelledby':'chart-title chart-desc'});
        root.append(svg('title',{id:'chart-title'},metrica === 'faturamento' ? 'Evolução do faturamento realizado' : 'Evolução da quantidade de pedidos'));
        root.append(svg('desc',{id:'chart-desc'},'Os valores exatos estão disponíveis na tabela Consultar dados do gráfico.'));
        const width=574,left=67,top=20,height=180;
        const maior = Math.max(...serie.map(p=>Number(p[metrica])),1);
        const base = Math.pow(10, Math.floor(Math.log10(maior)));
        const max = Math.ceil(maior/base)*base;
        const pontos = serie.map((p,i)=>[left+(serie.length===1?width/2:i*width/(serie.length-1)),top+height*(1-Number(p[metrica])/max)]);
        for (let i=0;i<=4;i++) { const y=top+height*i/4; const valor=max*(1-i/4); root.append(svg('line',{x1:left,y1:y,x2:left+width,y2:y,stroke:'#edf1f5','stroke-dasharray':'3 5'})); root.append(svg('text',{x:left-12,y:y+4,'text-anchor':'end'},metrica==='faturamento'?new Intl.NumberFormat('pt-BR',{notation:'compact',style:'currency',currency:'BRL',maximumFractionDigits:1}).format(valor):inteiro.format(valor))); }
        const defs = svg('defs',{}), gradient=svg('linearGradient',{id:'sales-fill',x1:'0',y1:'0',x2:'0',y2:'1'});
        gradient.append(svg('stop',{offset:'0%','stop-color':'#ed7044','stop-opacity':'.20'}),svg('stop',{offset:'100%','stop-color':'#ed7044','stop-opacity':'.01'})); defs.append(gradient); root.append(defs);
        if (pontos.length) {
            const path = pontos.map((p,i)=>`${i?'L':'M'}${p[0]},${p[1]}`).join(' ');
            root.append(svg('path',{d:`${path} L${pontos.at(-1)[0]},${top+height} L${pontos[0][0]},${top+height} Z`,fill:'url(#sales-fill)'}));
            root.append(svg('path',{d:path,fill:'none',stroke:'#ed7044','stroke-width':'2.5','stroke-linejoin':'round','stroke-linecap':'round'}));
            pontos.forEach((p,i)=> { const c=svg('circle',{cx:p[0],cy:p[1],r:serie.length>60?1.5:3,fill:'#fff',stroke:'#ed7044','stroke-width':1.8}); c.append(svg('title',{},`${dataLocal(serie[i].data)}: ${metrica==='faturamento'?brl.format(serie[i][metrica]):inteiro.format(serie[i][metrica])}`));root.append(c); });
            const indices = [...new Set(Array.from({length:Math.min(6,serie.length)},(_,i)=>Math.round(i*(serie.length-1)/Math.max(1,Math.min(6,serie.length)-1))))];
            indices.forEach(i=>root.append(svg('text',{x:pontos[i][0],y:228,'text-anchor':'middle'},dataLocal(serie[i].data).slice(0,5))));
        }
        $('grafico-vendas').replaceChildren(root);
        $('legenda-metrica').textContent=metrica==='faturamento'?'Faturamento realizado':'Pedidos recebidos';
    }
    function desenharSituacoes(d) {
        let acumulado=0;
        const total=d.indicadores.pedidos;
        const segmentos=[]; const ul=$('situacoes'); ul.replaceChildren();
        d.situacoes.forEach(s=>{
            const color=colors[s.status]||'#8b95a3'; const inicio=acumulado; acumulado+=total?s.quantidade/total*100:0;
            if(s.quantidade) segmentos.push(`${color} ${inicio}% ${acumulado}%`);
            const li=node('li'), label=node('span'), dot=node('i',null,'legend-dot'); dot.style.background=color;
            label.append(dot,document.createTextNode(labels[s.status]||'Outra situação'));li.append(label,node('b',inteiro.format(s.quantidade)));ul.append(li);
        });
        $('donut').style.background=segmentos.length?`conic-gradient(${segmentos.join(',')})`:'#edf0f5';
        $('donut-total').textContent=inteiro.format(total);
    }
    function ranking(produtos) {
        const ol=$('ranking'); ol.replaceChildren();
        if(!produtos.length) { ol.append(node('li','Ainda não há produtos em pedidos finalizados.','table-empty')); return; }
        produtos.forEach((p,i)=>{
            const li=node('li'),produto=node('div',null,'rank-product'),track=node('div',null,'rank-track'),bar=node('span');
            bar.style.width=`${p.quantidade/produtos[0].quantidade*100}%`;track.append(bar);
            const nome=node('strong',p.nome);nome.title=p.nome;produto.append(nome,track);
            const valor=node('div',`${inteiro.format(p.quantidade)} un.`, 'rank-metric');valor.append(node('small',brl.format(p.receitaBruta)));
            li.append(node('span',String(i+1).padStart(2,'0'),'rank-number'),produto,valor);ol.append(li);
        });
    }
    function renderizarPedidos(d) {
        totalPaginas=d.totalPaginas; pagina=d.pagina;
        const tbody=$('pedidos-tabela'); tbody.replaceChildren();
        if(!d.itens.length) {const tr=node('tr'),td=node('td','Nenhum pedido encontrado.','table-empty');td.colSpan=3;tr.append(td);tbody.append(tr);}
        d.itens.forEach(p=>{
            const tr=node('tr'),id=node('td',`#${p.id.slice(-7).toUpperCase()}`);id.title=p.id;
            id.append(node('small',new Date(p.dataHoraEntrada).toLocaleString('pt-BR',{timeZone:'America/Sao_Paulo',day:'2-digit',month:'2-digit',hour:'2-digit',minute:'2-digit'})));
            const td=node('td'),badge=node('span',null,'status-badge'),dot=node('i');dot.style.background=colors[p.status]||'#8b95a3';
            badge.append(dot,document.createTextNode(labels[p.status]||'Outra situação'));td.append(badge);
            tr.append(id,td,node('td',brl.format(p.valorTotal),'align-right'));tbody.append(tr);
        });
        $('paginacao-info').textContent=d.total?`${pagina*8+1}–${Math.min((pagina+1)*8,d.total)} de ${inteiro.format(d.total)} pedidos`:'0 pedidos';
        $('anterior').disabled=pagina<=0; $('proxima').disabled=pagina+1>=totalPaginas;
    }
    async function mudarPagina(delta) {
        if(carregando||carregandoPagina) return;
        const destino=pagina+delta;if(destino<0||destino>=totalPaginas)return;
        carregandoPagina=true;
        const minhaVersao=versao;
        $('anterior').disabled=true;$('proxima').disabled=true;
        try { const d=await json(`/api/dashboard/pedidos?${consulta}&pagina=${destino}&tamanho=8`,controller?.signal);if(minhaVersao===versao)renderizarPedidos(d); }
        catch(e){if(minhaVersao===versao&&!controller?.signal.aborted)erro(e.message);}
        finally{carregandoPagina=false;if(minhaVersao===versao){$('anterior').disabled=pagina<=0;$('proxima').disabled=pagina+1>=totalPaginas;}}
    }
    function preset(valor) {
        $('fim').value=hoje;$('inicio').value=valor==='mes'?`${hoje.slice(0,7)}-01`:deslocar(hoje,1-Number(valor));
        document.querySelectorAll('[data-dias]').forEach(b=>b.classList.toggle('selected',b.dataset.dias===valor));
    }
    async function iniciar() {
        try {
            const opcoes=await json('/api/dashboard/filtros');hoje=opcoes.hoje;
            $('status').replaceChildren(node('option','Todos os pedidos'));$('status').firstChild.value='';
            opcoes.status.forEach(s=>{const o=node('option',labels[s]||s);o.value=s;$('status').append(o);});
            preset('30');
            const url=new URLSearchParams(location.search);
            ['inicio','fim','status','agrupamento'].forEach(id=>{if(url.has(id))$(id).value=url.get(id);});
            if(url.size)document.querySelectorAll('[data-dias]').forEach(b=>b.classList.remove('selected'));
            await atualizar(true);
        } catch(e) { erro(e.message);$('atualizacao').textContent='Não foi possível carregar os filtros';$('resultados').hidden=true; }
    }
    $('filtros').addEventListener('submit',event=>{event.preventDefault();atualizar(true);});
    $('atualizar').addEventListener('click',()=>atualizar());$('tentar').addEventListener('click',()=>atualizar());
    $('anterior').addEventListener('click',()=>mudarPagina(-1));$('proxima').addEventListener('click',()=>mudarPagina(1));
    document.querySelectorAll('[data-dias]').forEach(b=>b.addEventListener('click',()=>{if(hoje){preset(b.dataset.dias);atualizar(true);}}));
    ['inicio','fim'].forEach(id=>$(id).addEventListener('change',()=>document.querySelectorAll('[data-dias]').forEach(b=>b.classList.remove('selected'))));
    document.querySelectorAll('[data-metrica]').forEach(b=>b.addEventListener('click',()=>{
        metrica=b.dataset.metrica;document.querySelectorAll('[data-metrica]').forEach(btn=>{btn.classList.toggle('selected',btn===b);btn.setAttribute('aria-pressed',String(btn===b));});desenharGrafico();
    }));
    setInterval(()=>{if(!document.hidden&&!carregando&&!carregandoPagina)atualizar();},60000);
    iniciar();
})();
