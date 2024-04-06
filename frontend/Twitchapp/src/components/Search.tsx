import { useEffect, useState } from "react";
import { Channel } from "./ChannelGrid";
import { getIsUserLoggedIn, logOutUser } from "../services/session";
import { searchChannel } from "../services/api-client";
import { Center, HStack, SimpleGrid } from "@chakra-ui/react";
import ChannelCard from "./ChannelCard";

interface SearchProps {
  searchText: string;
}

const Search = ({ searchText} : SearchProps ) => {
    const [channels, setchannel] = useState<Channel[]>([]);

    useEffect(() => {
    if (getIsUserLoggedIn()) {
      searchChannel(searchText)
        .then((res) => {
          setchannel(res.data);
        })
        .catch((err) => {
          if (
            err.hasOwnProperty("response") &&
            err.response.hasOwnProperty("data") &&
            (err.response.data.status == 401 || err.response.data.status == 400 || err.response.status == 401 || err.response.status == 400)
          ) {
            logOutUser();
            location.reload();
          }
        });
    }
  }, [searchText]);

  console.log("channels ", channels);
  console.log("typeof ",  typeof(channels));

  if(channels.length !== 0){
    return (
      <>
        <Center>
          <SimpleGrid columns={3} spacing={10} padding="10px">
            {channels.length > 0 &&
              channels.map((channel) => (
                <ChannelCard key={channel.id} channel={channel} />
              ))}
          </SimpleGrid>
        </Center>
      </>
    );
  }
  else
    return (
      <Center>
        <span className="noChannelsSubs">No Channels Found</span>
      </Center>
    );
  

}
export default Search;