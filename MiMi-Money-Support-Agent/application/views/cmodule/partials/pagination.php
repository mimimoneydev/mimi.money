<div v-show="content.total" class="list-pagination clearfix" style=" margin-bottom: 10px;">
    <a class="btn btn-success btn-sm" v-on="click: paginate(content.current_page-1)" v-attr="disabled: content.current_page == 1" href="javascript:void(0)">
        <i class="fa fa-angle-left"></i> Prev
    </a>
    <a class="btn btn-default btn-sm" v-repeat='pno:pagination' v-on="click: paginate(pno)" v-attr="disabled: content.current_page == pno" v-class="current: content.current_page == pno" href="javascript:void(0)">
        @{{pno}}
    </a>
    <a class="btn btn-success btn-sm" v-on="click: paginate(content.current_page+1)" v-attr="disabled: content.current_page == content.last_page" href="javascript:void(0)">
        Next <i class="fa fa-angle-right"></i>
    </a>
</div>