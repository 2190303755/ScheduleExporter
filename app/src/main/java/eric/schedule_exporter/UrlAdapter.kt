package eric.schedule_exporter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eric.schedule_exporter.databinding.ItemUrlBinding

class UrlAdapter(
    val navigator: (CharSequence) -> Unit
) : ListAdapter<UrlAdapter.Item, UrlAdapter.Holder>(
    object : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(old: Item, neo: Item) =
            old.url == neo.url

        override fun areContentsTheSame(old: Item, neo: Item) =
            old == neo
    }
) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = Holder(
        ItemUrlBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        this.navigator
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = this.getItem(position)
        val binding = holder.binding
        binding.url.text = item.url
        binding.name.text = item.name
    }

    class Holder(
        val binding: ItemUrlBinding,
        val navigator: (CharSequence) -> Unit
    ) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        init {
            this.binding.root.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val url = this.binding.url.text
            if (!url.isBlank()) {
                this.navigator(url)
            }
        }
    }

    data class Item(val name: String, val url: String)
}