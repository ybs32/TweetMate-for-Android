package com.ybsystem.tweethub.fragments.preference;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.ybsystem.tweethub.R;

public class VersionFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_description, container, false);
        setVersionText(view);
        return view;
    }

    private void setVersionText(View view) {
        String newVersion = getString(R.string.app_update_info);
        String oldVersion =
                "Ver.1.1.4 (2020/11/19)\n" +
                        "・メンション形式を修正\n" +
                        "\n" +
                        "Ver.1.1.3 (2020/9/12)\n" +
                        "・処理効率を改善\n" +
                        "・軽微な不具合修正\n" +
                        "        \n" +
                        "Ver.1.1.2 (2020/8/2)\n" +
                        "・レイアウトを修正\n" +
                        "・軽微な不具合修正\n" +
                        "\n" +
                        "Ver.1.1.1 (2020/7/27)\n" +
                        "・レイアウトを修正\n" +
                        "\n" +
                        "Ver.1.1.0 (2020/7/21)\n" +
                        "・アプリを最適化\n" +
                        "\n" +
                        "Ver.1.0.0 (2020/6/24)\n" +
                        "・GooglePlayストアへ移転\n";

        TextView tv = view.findViewById(R.id.text_description);
        tv.setText(newVersion + "\n" + oldVersion);
    }

}
