package com.example.file_manager.fragment.listAllFile

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.file_manager.CreateFolderDialog
import com.example.file_manager.R
import com.example.file_manager.SortByDialog
import com.example.file_manager.activity.FolderDetailActivity
import com.example.file_manager.inf.OnBackPressed
import com.example.file_manager.databinding.FragmentFileListBinding
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.io.File


class FileListFragment : Fragment(), OnBackPressed {
    private lateinit var binding: FragmentFileListBinding
    val gridLayout = GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false)
    val listLayout = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.plant(DebugTree())
        binding = FragmentFileListBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val typeOfFolder = FileListViewModel.typeOfFolder
        if(typeOfFolder == "other" || typeOfFolder == "download")
        {
            binding.btnAddFolder.visibility = View.VISIBLE
        }
        else
        {
            binding.btnAddFolder.visibility = View.GONE
        }

        val adapter = AllFileAdapter(requireContext()){
            FileListViewModel.openFolder(it)
        }

        adapter.setHandleMenuMode(object :ClassHandleMenuMode(){
            //Set giao di???n MenuMode kkhi b???t v?? t???t
            override fun changeMenuMode(mode: MenuMode) {
                if(mode == MenuMode.OPEN)
                {
                    binding.menu.visibility = View.GONE
                    FileListViewModel.menuMode = MenuMode.OPEN
                    binding.btnAddFolder.visibility = View.VISIBLE
                }
                else if(mode == MenuMode.SELECT)
                {
                    binding.menu.visibility = View.VISIBLE
                    FileListViewModel.menuMode = mode
                    binding.btnAddFolder.visibility = View.GONE

                    binding.btnCut.visibility = View.VISIBLE
                    binding.btnCopy.visibility = View.VISIBLE
                    binding.btnShare.visibility = View.VISIBLE
                    binding.btnClose.visibility = View.GONE
                }
                else if(mode == MenuMode.PASTE)
                {
                    binding.menu.visibility = View.VISIBLE
                    FileListViewModel.menuMode = mode
                    binding.btnAddFolder.visibility = View.GONE

                    binding.btnCopy.visibility = View.GONE
                    binding.btnShare.visibility = View.GONE
                    binding.btnClose.visibility = View.VISIBLE

                }
            }
        })
        //setOnClick c??c n??t
        binding.btnCopy.setOnClickListener{
            FileListViewModel.copy()
            setPasteMode()
        }
        binding.btnPaste.setOnClickListener {
            FileListViewModel.paste()
            setOpenMode()
        }
        binding.btnCut.setOnClickListener{
            FileListViewModel.cut()
            setPasteMode()
        }
        binding.btnClose.setOnClickListener{
            setOpenMode()
            FileListViewModel.selectedFile = File("")
            binding.rcvAllFile.adapter?.notifyDataSetChanged()
        }
        binding.btnDelete.setOnClickListener{

            val dialogBuilder = AlertDialog.Builder(context)
            dialogBuilder.setMessage("Do you want to delete this folder ?")
                .setCancelable(false)
                .setPositiveButton("Delete", DialogInterface.OnClickListener {
                        dialog, id -> FileListViewModel.delete()
                    setOpenMode()
                })
                .setNegativeButton("Cancel", DialogInterface.OnClickListener {
                        dialog, id ->
                    FileListViewModel.selectedFile = File("")
                    setOpenMode()
                    dialog.cancel()
                })

            val alert = dialogBuilder.create()
            alert.setTitle("Delete folder")
            alert.show()
        }
        binding.btnShare.setOnClickListener {
            context?.let {
                FileListViewModel.share(it)
            }
        }
        binding.btnAddFolder.setOnClickListener {
            val createFolderDialog = CreateFolderDialog()
            createFolderDialog.show(parentFragmentManager, "create folder" )
        }
        binding.imgSort.setOnClickListener {
            val sortByDialog = SortByDialog()
            sortByDialog.show(parentFragmentManager, "sort")
        }
        //set Adapter cho recyclerview
        binding.rcvAllFile.layoutManager = listLayout
        binding.rcvAllFile.adapter = adapter
        FileListViewModel.files.observe(viewLifecycleOwner){
            adapter.listFile = it
            //ki???m tra folder c?? tr???ng kh??ng, tr???ng hi???n textView "NoFlie"
            if (it.isNotEmpty()){
                Timber.d("It has files")
                FileListViewModel.changeStateLoading()
                binding.rcvAllFile.visibility = View.VISIBLE
                binding.loading.visibility = View.GONE
                binding.txtNoFile.visibility = View.GONE
            }
            else{
                FileListViewModel.changeStateLoading()
                Timber.d("It has no files")
                binding.txtNoFile.visibility = View.VISIBLE
                binding.loading.visibility = View.GONE
                binding.rcvAllFile.visibility = View.GONE
            }
        }

        (activity as FolderDetailActivity).onBackPressed = this
        // s??t ch??? ????? hi???n th??? hi???n d???ng list hay d???ng l?????i
        binding.imgLayoutChange.setOnClickListener{
            FileListViewModel.changeLayout()
            FileListViewModel.isGrid.observe(viewLifecycleOwner){
                if (it) {
                    Timber.d("change to list")
                    binding.imgLayoutChange.setImageResource(R.drawable.ic_grid_on)
                    binding.rcvAllFile.layoutManager = listLayout
                }
                else {
                    Timber.d("change to grid")
                    binding.imgLayoutChange.setImageResource(R.drawable.ic_list)
                    binding.rcvAllFile.layoutManager = gridLayout
                }
            }
        }
        //animation loading
        FileListViewModel.stateLoading.observe(viewLifecycleOwner){
            if (it){
                binding.loading.visibility = View.VISIBLE
                binding.rcvAllFile.visibility = View.GONE
                binding.txtNoFile.visibility = View.GONE
            }
        }
    }
    //override h??m onClick c???a interface OnBackPressed x??? l?? event back c???a Device
    override fun onClick() {
        Timber.d("Clicked back")
        if(FileListViewModel.menuMode == MenuMode.SELECT)
        {
            setOpenMode()
            FileListViewModel.selectedFile = File("")


        }
        else
        {
            FileListViewModel.onBackPressed()
        }
    }
    //override h??m isClosed c???a interface OnBackPressed x??? l?? event back c???a Device
    override fun isClosed(): Boolean {
        if (FileListViewModel.isRoot()){
            if( FileListViewModel.menuMode == MenuMode.OPEN) {
                FileListViewModel.clear()
                return true
            }
            else if(FileListViewModel.menuMode == MenuMode.PASTE)
            {
                FileListViewModel.menuMode=MenuMode.SELECT
                return false
            }
        }

        return false
    }
    //Ch??? ????? m??? file
    fun setOpenMode()
    {
        binding.menu.visibility = View.GONE
        FileListViewModel.menuMode = MenuMode.OPEN
        binding.btnAddFolder.visibility = View.VISIBLE
        binding.rcvAllFile.adapter?.notifyDataSetChanged()
    }
    // ch??? ????? paste File
    fun setPasteMode()
    {
        binding.menu.visibility = View.VISIBLE
        FileListViewModel.menuMode = MenuMode.PASTE
        binding.btnAddFolder.visibility = View.GONE
        binding.btnCut.visibility = View.INVISIBLE
        binding.btnCopy.visibility = View.GONE
        binding.btnShare.visibility = View.GONE
        binding.btnClose.visibility = View.VISIBLE
    }

}