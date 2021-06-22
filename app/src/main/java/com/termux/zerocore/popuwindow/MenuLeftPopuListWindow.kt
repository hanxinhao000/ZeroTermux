package com.termux.zerocore.popuwindow

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xh_lib.utils.UUtils
import com.termux.R

public class MenuLeftPopuListWindow :BasePuPuWindow{

    private var recycler_view:RecyclerView? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun initView(mView: View) {

        recycler_view = mView.findViewById(R.id.recycler_view)

    }

    override fun getViewId(): Int {

        return R.layout.popu_window_menu_left
    }


    public fun setListData(mList:ArrayList<MenuLeftPopuListData>){

        recycler_view?.layoutManager = GridLayoutManager(UUtils.getContext(),2)

        recycler_view?.adapter = MenuLeftPopuListWindowAdapter(mList,mItemClickPopuListener,this)

    }


    class MenuLeftPopuListWindowAdapter:RecyclerView.Adapter<MenuLeftPopuListWindowViewHolder>{

        private lateinit var mList:ArrayList<MenuLeftPopuListData>
        private var mItemClickPopuListener:ItemClickPopuListener? = null
        private var mMenuLeftPopuListWindow:MenuLeftPopuListWindow? = null
        constructor(mList:ArrayList<MenuLeftPopuListData>,mItemClickPopuListener:ItemClickPopuListener?,mMenuLeftPopuListWindow:MenuLeftPopuListWindow) : super(){
            this.mList = mList
            this.mItemClickPopuListener = mItemClickPopuListener
            this.mMenuLeftPopuListWindow = mMenuLeftPopuListWindow

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuLeftPopuListWindowViewHolder {

            return MenuLeftPopuListWindowViewHolder(UUtils.getViewLayViewGroup(R.layout.item_popu_windows,parent))
        }

        override fun onBindViewHolder(holder: MenuLeftPopuListWindowViewHolder, position: Int) {

            val menuLeftPopuListData = mList[position]

            holder.img.setImageResource(menuLeftPopuListData.imgId)
            holder.title.text = menuLeftPopuListData.titleString

            holder.itemView.setOnClickListener {

                mItemClickPopuListener?.itemClick(menuLeftPopuListData.id,position,mMenuLeftPopuListWindow)

            }


        }

        override fun getItemCount(): Int {

            return mList.size
        }
    }



    private var mItemClickPopuListener:ItemClickPopuListener? = null

    public fun setItemClickPopuListener(mItemClickPopuListener:ItemClickPopuListener){

        this.mItemClickPopuListener = mItemClickPopuListener

    }

    public interface ItemClickPopuListener{


        fun itemClick(id:Int,index:Int,mMenuLeftPopuListWindow:MenuLeftPopuListWindow?)



    }


    class MenuLeftPopuListWindowViewHolder:RecyclerView.ViewHolder{

        public lateinit var img:ImageView
        public lateinit var title:TextView

        constructor(itemView: View) : super(itemView){
            img = itemView.findViewById(R.id.img)
            title = itemView.findViewById(R.id.title)

        }
    }




    public data class MenuLeftPopuListData(

        var imgId:Int,
        var titleString:String,
        var id:Int


    )

}
